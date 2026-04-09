package com.teamdesk.agent.dto;

public class AgentHeartbeatRequest {

    private String machineId;
    private String ip;

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}