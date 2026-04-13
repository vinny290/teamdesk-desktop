package com.teamdesk.agent.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamdesk.agent.config.AgentConfig;
import com.teamdesk.agent.consent.ConsentService;
import com.teamdesk.agent.dto.AgentSignalEnvelope;
import com.teamdesk.agent.dto.InputEventPayload;
import com.teamdesk.agent.input.RemoteInputService;
import com.teamdesk.agent.session.AgentSessionState;
import com.teamdesk.agent.ui.AgentInfoWindow;
import com.teamdesk.agent.util.JsonMapperFactory;
import com.teamdesk.agent.webrtc.AgentPeerConnectionService;
import com.teamdesk.agent.webrtc.WebRtcSignalSender;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentWebSocketClient extends WebSocketListener implements WebRtcSignalSender {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketClient.class);

    private final AgentConfig config;
    private final ConsentService consentService;
    private final AgentSessionState sessionState;
    private final RemoteInputService remoteInputService;
    private final AgentPeerConnectionService peerConnectionService;
    private final AgentInfoWindow infoWindow;
    private final ObjectMapper objectMapper = JsonMapperFactory.create();
    private final OkHttpClient httpClient = HttpClientFactory.create();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();

    private volatile WebSocket webSocket;
    private volatile boolean manualClose = false;
    private volatile boolean reconnectScheduled = false;

    public AgentWebSocketClient(
            AgentConfig config,
            ConsentService consentService,
            AgentSessionState sessionState,
            RemoteInputService remoteInputService,
            AgentInfoWindow infoWindow
    ) {
        this.config = config;
        this.consentService = consentService;
        this.sessionState = sessionState;
        this.remoteInputService = remoteInputService;
        this.infoWindow = infoWindow;
        this.peerConnectionService = new AgentPeerConnectionService(sessionState, this);
    }

    public synchronized void connect() {
        manualClose = false;

        Request request = new Request.Builder()
                .url(config.getServerWsUrl())
                .build();

        log.info("Connecting to signaling server: {}", config.getServerWsUrl());
        this.webSocket = httpClient.newWebSocket(request, this);
    }

    public synchronized void close() {
        manualClose = true;

        if (webSocket != null) {
            log.info("Closing WebSocket connection");
            webSocket.close(1000, "Agent shutdown");
        }

        reconnectExecutor.shutdownNow();
        peerConnectionService.reset();
        sessionState.clear();
        infoWindow.setViewerConnected(false, null);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        reconnectScheduled = false;
        log.info("WebSocket connected");

        try {
            AgentSignalEnvelope envelope = new AgentSignalEnvelope();
            envelope.setType("REGISTER_AGENT");
            envelope.setMachineId(config.getMachineId());

            String json = objectMapper.writeValueAsString(envelope);
            webSocket.send(json);

            log.info("REGISTER_AGENT sent. machineId={}", config.getMachineId());
        } catch (Exception e) {
            log.error("Failed to send REGISTER_AGENT", e);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        log.debug("WS message received: {}", text);

        try {
            AgentSignalEnvelope envelope = objectMapper.readValue(text, AgentSignalEnvelope.class);
            handleMessage(webSocket, envelope);
        } catch (Exception e) {
            log.error("Failed to process WS message: {}", text, e);
        }
    }

    private void handleMessage(WebSocket webSocket, AgentSignalEnvelope envelope) throws Exception {
        String type = envelope.getType();

        if ("REQUEST_CONSENT".equals(type)) {
            handleConsentRequest(webSocket, envelope);
            return;
        }

        if ("INPUT_EVENT".equals(type)) {
            handleInputEvent(envelope);
            return;
        }

        if ("SDP_OFFER".equals(type)) {
            handleSdpOffer(envelope);
            return;
        }

        if ("ICE_CANDIDATE".equals(type)) {
            handleIceCandidate(envelope);
            return;
        }

        if ("STOP_SESSION".equals(type)) {
            handleStopSession(envelope);
            return;
        }

        log.warn("Unsupported WS message type received: {}", type);
    }

    private void handleConsentRequest(WebSocket webSocket, AgentSignalEnvelope envelope) throws Exception {
        if (isBlank(envelope.getViewerId()) || isBlank(envelope.getMachineId())) {
            log.warn("Invalid REQUEST_CONSENT received: {}", objectMapper.writeValueAsString(envelope));
            return;
        }

        log.info("REQUEST_CONSENT received. viewerId={}, machineId={}, sessionId={}",
                envelope.getViewerId(), envelope.getMachineId(), envelope.getSessionId());

        sessionState.openPendingSession(
                envelope.getSessionId(),
                envelope.getViewerId(),
                envelope.getMachineId()
        );

        log.info("Showing consent dialog for viewerId={}", envelope.getViewerId());

        boolean granted = consentService.requestConsent(
                "Разрешить удалённое подключение для viewer: " + envelope.getViewerId() + "?"
        );

        log.info("Consent dialog completed. granted={}, viewerId={}", granted, envelope.getViewerId());

        AgentSignalEnvelope response = new AgentSignalEnvelope();
        response.setType(granted ? "CONSENT_GRANTED" : "CONSENT_DECLINED");
        response.setSessionId(envelope.getSessionId());
        response.setViewerId(envelope.getViewerId());
        response.setMachineId(envelope.getMachineId());

        if (granted) {
            sessionState.markConsentGranted();
            infoWindow.setViewerConnected(true, envelope.getViewerId());
            log.info("Consent granted. state={}", sessionState);
        } else {
            sessionState.markConsentDeclined();
            infoWindow.setViewerConnected(false, null);
            log.info("Consent declined. state={}", sessionState);
        }

        webSocket.send(objectMapper.writeValueAsString(response));
        log.info("Consent response sent: {}", response.getType());

        if (!granted) {
            sessionState.clear();
        }
    }

    private void handleInputEvent(AgentSignalEnvelope envelope) throws Exception {
        if (!sessionState.isConsentGranted()) {
            log.warn("Ignoring INPUT_EVENT because consent is not granted");
            return;
        }

        if (isBlank(envelope.getPayload())) {
            log.warn("Ignoring INPUT_EVENT because payload is empty");
            return;
        }

        InputEventPayload payload = objectMapper.readValue(envelope.getPayload(), InputEventPayload.class);
        remoteInputService.handle(payload);
    }

    private void handleSdpOffer(AgentSignalEnvelope envelope) throws Exception {
        if (!sessionState.isConsentGranted()) {
            log.warn("Ignoring SDP_OFFER because consent is not granted");
            return;
        }

        if (isBlank(envelope.getSessionId())) {
            log.warn("Ignoring SDP_OFFER because sessionId is empty");
            return;
        }

        if (isBlank(envelope.getViewerId()) || isBlank(envelope.getMachineId()) || isBlank(envelope.getSdp())) {
            log.warn("Ignoring invalid SDP_OFFER");
            return;
        }

        log.info(
                "Handling SDP_OFFER. sessionId={}, viewerId={}, machineId={}, sdpLength={}",
                envelope.getSessionId(),
                envelope.getViewerId(),
                envelope.getMachineId(),
                envelope.getSdp().length()
        );

        peerConnectionService.handleOffer(
                envelope.getSessionId(),
                envelope.getMachineId(),
                envelope.getViewerId(),
                envelope.getSdp()
        );
    }

    private void handleIceCandidate(AgentSignalEnvelope envelope) {
        if (!sessionState.isConsentGranted()) {
            log.warn("Ignoring ICE_CANDIDATE because consent is not granted");
            return;
        }

        if (isBlank(envelope.getSessionId())) {
            log.warn("Ignoring ICE_CANDIDATE because sessionId is empty");
            return;
        }

        if (isBlank(envelope.getViewerId()) || isBlank(envelope.getMachineId()) || isBlank(envelope.getCandidate())) {
            log.warn("Ignoring invalid ICE_CANDIDATE");
            return;
        }

        log.info(
                "Handling remote ICE_CANDIDATE. sessionId={}, viewerId={}, machineId={}, sdpMid={}, sdpMLineIndex={}",
                envelope.getSessionId(),
                envelope.getViewerId(),
                envelope.getMachineId(),
                envelope.getSdpMid(),
                envelope.getSdpMLineIndex()
        );

        peerConnectionService.handleRemoteIceCandidate(
                envelope.getSessionId(),
                envelope.getMachineId(),
                envelope.getViewerId(),
                envelope.getCandidate(),
                envelope.getSdpMid(),
                envelope.getSdpMLineIndex()
        );
    }

    private void handleStopSession(AgentSignalEnvelope envelope) {
        log.info("STOP_SESSION received. sessionId={}, viewerId={}, machineId={}",
                envelope.getSessionId(), envelope.getViewerId(), envelope.getMachineId());

        peerConnectionService.reset();
        sessionState.clear();
        infoWindow.setViewerConnected(false, null);

        log.info("Remote session stopped and local state cleared");
    }

    @Override
    public void sendSdpAnswer(String sessionId, String machineId, String viewerId, String sdp) throws Exception {
        ensureSocketOpen();

        AgentSignalEnvelope envelope = new AgentSignalEnvelope();
        envelope.setType("SDP_ANSWER");
        envelope.setSessionId(sessionId);
        envelope.setMachineId(machineId);
        envelope.setViewerId(viewerId);
        envelope.setSdp(sdp);

        webSocket.send(objectMapper.writeValueAsString(envelope));

        log.info(
                "SDP_ANSWER sent. sessionId={}, viewerId={}, machineId={}, sdpLength={}",
                sessionId, viewerId, machineId, sdp != null ? sdp.length() : 0
        );
    }

    @Override
    public void sendIceCandidate(
            String sessionId,
            String machineId,
            String viewerId,
            String candidate,
            String sdpMid,
            Integer sdpMLineIndex
    ) throws Exception {
        ensureSocketOpen();

        AgentSignalEnvelope envelope = new AgentSignalEnvelope();
        envelope.setType("ICE_CANDIDATE");
        envelope.setSessionId(sessionId);
        envelope.setMachineId(machineId);
        envelope.setViewerId(viewerId);
        envelope.setCandidate(candidate);
        envelope.setSdpMid(sdpMid);
        envelope.setSdpMLineIndex(sdpMLineIndex);

        webSocket.send(objectMapper.writeValueAsString(envelope));

        log.info(
                "Local ICE_CANDIDATE sent. sessionId={}, viewerId={}, machineId={}, sdpMid={}, sdpMLineIndex={}",
                sessionId, viewerId, machineId, sdpMid, sdpMLineIndex
        );
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        log.warn("WebSocket closing. code={}, reason={}", code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        log.warn("WebSocket closed. code={}, reason={}", code, reason);
        peerConnectionService.reset();
        sessionState.clear();
        infoWindow.setViewerConnected(false, null);
        scheduleReconnectIfNeeded();
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        if (response != null) {
            log.error("WebSocket failure. code={}, message={}", response.code(), response.message(), t);
        } else {
            log.error("WebSocket failure without HTTP response", t);
        }

        peerConnectionService.reset();
        sessionState.clear();
        infoWindow.setViewerConnected(false, null);
        scheduleReconnectIfNeeded();
    }

    private void ensureSocketOpen() {
        if (webSocket == null) {
            throw new IllegalStateException("WebSocket is not initialized");
        }
    }

    private synchronized void scheduleReconnectIfNeeded() {
        if (manualClose) {
            log.info("Reconnect skipped because close was manual");
            return;
        }

        if (reconnectScheduled) {
            return;
        }

        reconnectScheduled = true;

        log.warn("Scheduling reconnect in 3 seconds");
        reconnectExecutor.schedule(() -> {
            try {
                reconnectScheduled = false;
                connect();
            } catch (Exception e) {
                log.error("Reconnect attempt failed", e);
                scheduleReconnectIfNeeded();
            }
        }, 3, TimeUnit.SECONDS);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}