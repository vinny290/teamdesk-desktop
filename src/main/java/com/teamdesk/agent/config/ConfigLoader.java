package com.teamdesk.agent.config;

public class ConfigLoader {

    public static AgentConfig load() {
        AgentConfig config = new AgentConfig();
        config.setServerHttpBaseUrl("http://localhost:8080");
        config.setServerWsUrl("ws://localhost:8080/ws/signaling");
        config.setHeartbeatIntervalSeconds(5);
        config.setMachineId("desktop-agent-1");
        return config;
    }
}