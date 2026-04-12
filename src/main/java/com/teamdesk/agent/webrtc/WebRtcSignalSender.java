package com.teamdesk.agent.webrtc;

public interface WebRtcSignalSender {

    void sendSdpAnswer(String sessionId, String machineId, String viewerId, String sdp) throws Exception;

    void sendIceCandidate(
            String sessionId,
            String machineId,
            String viewerId,
            String candidate,
            String sdpMid,
            Integer sdpMLineIndex
    ) throws Exception;
}