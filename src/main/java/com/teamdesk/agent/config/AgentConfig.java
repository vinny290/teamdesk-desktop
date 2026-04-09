package com.teamdesk.agent.config;

public class AgentConfig {

    private String serverHttpBaseUrl;
    private String serverWsUrl;
    private int heartbeatIntervalSeconds;
    private String machineId;

    public String getServerHttpBaseUrl() {
        return serverHttpBaseUrl;
    }

    public void setServerHttpBaseUrl(String serverHttpBaseUrl) {
        this.serverHttpBaseUrl = serverHttpBaseUrl;
    }

    public String getServerWsUrl() {
        return serverWsUrl;
    }

    public void setServerWsUrl(String serverWsUrl) {
        this.serverWsUrl = serverWsUrl;
    }

    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }
}