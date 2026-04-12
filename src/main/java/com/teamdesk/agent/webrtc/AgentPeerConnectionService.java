package com.teamdesk.agent.webrtc;

import com.teamdesk.agent.session.AgentSessionState;
import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCAnswerOptions;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCIceGatheringState;
import dev.onvoid.webrtc.RTCIceServer;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCPeerConnectionState;
import dev.onvoid.webrtc.RTCRtpReceiver;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.RTCSignalingState;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.MediaStream;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.video.VideoDesktopSource;
import dev.onvoid.webrtc.media.video.VideoTrack;
import dev.onvoid.webrtc.media.video.desktop.DesktopSource;
import dev.onvoid.webrtc.media.video.desktop.ScreenCapturer;
import dev.onvoid.webrtc.media.video.desktop.WindowCapturer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AgentPeerConnectionService implements PeerConnectionObserver {

    private static final Logger log = LoggerFactory.getLogger(AgentPeerConnectionService.class);

    private static final String VIDEO_TRACK_ID = "screen-video-0";
    private static final String STREAM_ID = "screen-stream-0";

    private final AgentSessionState sessionState;
    private final WebRtcSignalSender signalSender;
    private final PeerConnectionFactory factory;

    private final List<RemoteIceCandidate> pendingRemoteCandidates = new ArrayList<>();

    private volatile RTCPeerConnection peerConnection;
    private volatile VideoDesktopSource videoSource;
    private volatile VideoTrack videoTrack;

    private volatile String currentSessionId;
    private volatile String currentMachineId;
    private volatile String currentViewerId;

    public AgentPeerConnectionService(
            AgentSessionState sessionState,
            WebRtcSignalSender signalSender
    ) {
        this.sessionState = sessionState;
        this.signalSender = signalSender;
        this.factory = new PeerConnectionFactory();
    }

    public synchronized void handleOffer(
            String sessionId,
            String machineId,
            String viewerId,
            String sdpOffer
    ) throws Exception {
        validateRequired(sessionId, "sessionId");
        validateRequired(machineId, "machineId");
        validateRequired(viewerId, "viewerId");
        validateRequired(sdpOffer, "sdpOffer");

        if (!sessionState.isConsentGranted()) {
            log.warn("Ignoring SDP_OFFER because consent is not granted. sessionId={}", sessionId);
            return;
        }

        if (peerConnection != null && !sessionId.equals(currentSessionId)) {
            log.info(
                    "New session detected. Resetting previous peer. oldSessionId={}, newSessionId={}",
                    currentSessionId,
                    sessionId
            );
            reset();
        }

        currentSessionId = sessionId;
        currentMachineId = machineId;
        currentViewerId = viewerId;

        ensurePeerCreated(sessionId, machineId, viewerId);
        ensureDesktopVideoTrackCreated();

        sessionState.bindSession(sessionId, viewerId, machineId);
        sessionState.markSignalingReady();

        RTCSessionDescription remoteOffer = new RTCSessionDescription(RTCSdpType.OFFER, sdpOffer);

        log.info(
                "Applying remote SDP offer. sessionId={}, viewerId={}, machineId={}, sdpLength={}",
                sessionId,
                viewerId,
                machineId,
                sdpOffer.length()
        );

        peerConnection.setRemoteDescription(remoteOffer, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                log.info(
                        "Remote description set successfully. sessionId={}, viewerId={}, machineId={}",
                        sessionId,
                        viewerId,
                        machineId
                );

                flushPendingRemoteCandidates();

                RTCAnswerOptions answerOptions = new RTCAnswerOptions();

                peerConnection.createAnswer(answerOptions, new CreateSessionDescriptionObserver() {
                    @Override
                    public void onSuccess(RTCSessionDescription answer) {
                        log.info(
                                "SDP answer created successfully. sessionId={}, viewerId={}, machineId={}",
                                sessionId,
                                viewerId,
                                machineId
                        );

                        peerConnection.setLocalDescription(answer, new SetSessionDescriptionObserver() {
                            @Override
                            public void onSuccess() {
                                log.info(
                                        "Local description set successfully. sessionId={}, viewerId={}, machineId={}",
                                        sessionId,
                                        viewerId,
                                        machineId
                                );

                                try {
                                    signalSender.sendSdpAnswer(
                                            sessionId,
                                            machineId,
                                            viewerId,
                                            answer.sdp
                                    );
                                } catch (Exception e) {
                                    log.error("Failed to send SDP answer", e);
                                }
                            }

                            @Override
                            public void onFailure(String error) {
                                log.error(
                                        "Failed to set local description. sessionId={}, error={}",
                                        sessionId,
                                        error
                                );
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        log.error(
                                "Failed to create SDP answer. sessionId={}, error={}",
                                sessionId,
                                error
                        );
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                log.error(
                        "Failed to set remote description. sessionId={}, error={}",
                        sessionId,
                        error
                );
            }
        });
    }

    public synchronized void handleRemoteIceCandidate(
            String sessionId,
            String machineId,
            String viewerId,
            String candidate,
            String sdpMid,
            Integer sdpMLineIndex
    ) {
        validateRequired(sessionId, "sessionId");
        validateRequired(machineId, "machineId");
        validateRequired(viewerId, "viewerId");
        validateRequired(candidate, "candidate");

        if (!sessionState.isConsentGranted()) {
            log.warn("Ignoring ICE_CANDIDATE because consent is not granted. sessionId={}", sessionId);
            return;
        }

        if (peerConnection == null) {
            pendingRemoteCandidates.add(new RemoteIceCandidate(candidate, sdpMid, sdpMLineIndex));
            log.info(
                    "Peer is not ready yet. ICE candidate buffered. sessionId={}, pendingCount={}",
                    sessionId,
                    pendingRemoteCandidates.size()
            );
            return;
        }

        RTCIceCandidate iceCandidate = new RTCIceCandidate(sdpMid, sdpMLineIndex, candidate);
        peerConnection.addIceCandidate(iceCandidate);

        log.info(
                "Remote ICE candidate added. sessionId={}, viewerId={}, machineId={}, sdpMid={}, sdpMLineIndex={}",
                sessionId,
                viewerId,
                machineId,
                sdpMid,
                sdpMLineIndex
        );
    }

    public synchronized void reset() {
        log.info("Resetting AgentPeerConnectionService");

        pendingRemoteCandidates.clear();
        currentSessionId = null;
        currentMachineId = null;
        currentViewerId = null;

        if (videoSource != null) {
            try {
                videoSource.stop();
            } catch (Exception e) {
                log.warn("Error while stopping video source", e);
            }

            try {
                videoSource.dispose();
            } catch (Exception e) {
                log.warn("Error while disposing video source", e);
            }

            videoSource = null;
        }

        videoTrack = null;

        if (peerConnection != null) {
            try {
                peerConnection.close();
            } catch (Exception e) {
                log.warn("Error while closing peerConnection", e);
            }
            peerConnection = null;
        }

        sessionState.markStreaming(false);
    }

    private void ensurePeerCreated(String sessionId, String machineId, String viewerId) {
        if (peerConnection != null) {
            return;
        }

        RTCConfiguration config = new RTCConfiguration();

        RTCIceServer iceServer = new RTCIceServer();
        iceServer.urls.add("stun:stun.l.google.com:19302");
        config.iceServers.add(iceServer);

        peerConnection = factory.createPeerConnection(config, this);

        log.info(
                "PeerConnection created. sessionId={}, viewerId={}, machineId={}",
                sessionId,
                viewerId,
                machineId
        );
    }

    private void ensureDesktopVideoTrackCreated() {
        if (videoTrack != null) {
            return;
        }

        List<DesktopSource> screens;
        List<DesktopSource> windows;

        try {
            ScreenCapturer screenCapturer = new ScreenCapturer();
            WindowCapturer windowCapturer = new WindowCapturer();

            screens = screenCapturer.getDesktopSources();
            windows = windowCapturer.getDesktopSources();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to enumerate desktop sources", e);
        }

        videoSource = new VideoDesktopSource();

        // Для первого стабильного запуска лучше начать с умеренных значений.
        videoSource.setFrameRate(10);
        videoSource.setMaxFrameSize(1280, 720);

        if (!screens.isEmpty()) {
            DesktopSource selectedScreen = screens.get(0);
            log.info(
                    "Selected screen for capture: title='{}', id={}",
                    selectedScreen.title,
                    selectedScreen.id
            );
            videoSource.setSourceId(selectedScreen.id, false);
        } else if (!windows.isEmpty()) {
            DesktopSource selectedWindow = windows.get(0);
            log.info(
                    "Selected window for capture: title='{}', id={}",
                    selectedWindow.title,
                    selectedWindow.id
            );
            videoSource.setSourceId(selectedWindow.id, true);
        } else {
            log.warn("No desktop sources found. Falling back to sourceId=0 (primary screen)");
            videoSource.setSourceId(0, false);
        }

        videoSource.start();

        videoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);

        List<String> streamIds = new ArrayList<>();
        streamIds.add(STREAM_ID);

        peerConnection.addTrack(videoTrack, streamIds);

        sessionState.markStreaming(true);

        log.info("Desktop video source started and video track added");
    }

    private void flushPendingRemoteCandidates() {
        if (peerConnection == null || pendingRemoteCandidates.isEmpty()) {
            return;
        }

        log.info("Flushing buffered remote ICE candidates. count={}", pendingRemoteCandidates.size());

        for (RemoteIceCandidate candidate : pendingRemoteCandidates) {
            RTCIceCandidate rtcIceCandidate = new RTCIceCandidate(
                    candidate.sdpMid(),
                    candidate.sdpMLineIndex(),
                    candidate.candidate()
            );
            peerConnection.addIceCandidate(rtcIceCandidate);
        }

        pendingRemoteCandidates.clear();
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    @Override
    public void onIceCandidate(RTCIceCandidate candidate) {
        log.info(
                "Local ICE candidate generated. sessionId={}, viewerId={}, machineId={}, sdpMid={}, sdpMLineIndex={}",
                currentSessionId,
                currentViewerId,
                currentMachineId,
                candidate.sdpMid,
                candidate.sdpMLineIndex
        );

        if (currentSessionId == null || currentViewerId == null || currentMachineId == null) {
            log.warn("Skipping local ICE candidate because session context is incomplete");
            return;
        }

        try {
            signalSender.sendIceCandidate(
                    currentSessionId,
                    currentMachineId,
                    currentViewerId,
                    candidate.sdp,
                    candidate.sdpMid,
                    candidate.sdpMLineIndex
            );
        } catch (Exception e) {
            log.error("Failed to send local ICE candidate", e);
        }
    }

    @Override
    public void onConnectionChange(RTCPeerConnectionState state) {
        log.info("Peer connection state changed: {}", state);

        if (state == RTCPeerConnectionState.CONNECTED) {
            sessionState.markActive();
        }
    }

    @Override
    public void onIceConnectionChange(RTCIceConnectionState state) {
        log.info("ICE connection state changed: {}", state);
    }

    @Override
    public void onIceGatheringChange(RTCIceGatheringState state) {
        log.info("ICE gathering state changed: {}", state);
    }

    @Override
    public void onSignalingChange(RTCSignalingState state) {
        log.info("Signaling state changed: {}", state);
    }

    @Override
    public void onDataChannel(RTCDataChannel dataChannel) {
        log.info("Data channel created: {}", dataChannel.getLabel());
    }

    @Override
    public void onRenegotiationNeeded() {
        log.info("Renegotiation needed");
    }

    @Override
    public void onAddTrack(RTCRtpReceiver receiver, MediaStream[] mediaStreams) {
        log.info("Track added from remote peer: {}", receiver.getTrack().getKind());
    }

    @Override
    public void onRemoveTrack(RTCRtpReceiver receiver) {
        log.info("Track removed from remote peer: {}", receiver.getTrack().getKind());
    }

    @Override
    public void onTrack(RTCRtpTransceiver transceiver) {
        MediaStreamTrack track = transceiver.getReceiver().getTrack();
        log.info("onTrack fired: {}", track.getKind());
    }

    private record RemoteIceCandidate(
            String candidate,
            String sdpMid,
            Integer sdpMLineIndex
    ) {
    }
}