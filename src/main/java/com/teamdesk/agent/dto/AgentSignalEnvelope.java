package com.teamdesk.agent.dto;

public class AgentSignalEnvelope {

    private String type;
    private String sessionId;
    private String machineId;
    private String viewerId;

    private String sdp;
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;

    private String payload;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getViewerId() {
        return viewerId;
    }

    public void setViewerId(String viewerId) {
        this.viewerId = viewerId;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }

    public String getSdpMid() {
        return sdpMid;
    }

    public void setSdpMid(String sdpMid) {
        this.sdpMid = sdpMid;
    }

    public Integer getSdpMLineIndex() {
        return sdpMLineIndex;
    }

    public void setSdpMLineIndex(Integer sdpMLineIndex) {
        this.sdpMLineIndex = sdpMLineIndex;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}