package com.teamdesk.agent.config;

import com.teamdesk.agent.identity.MachineIdProvider;

public class ConfigLoader {

    public static AgentConfig load() {
        AgentConfig config = new AgentConfig();
        config.setServerHttpBaseUrl("http://192.168.1.10:8080");
        config.setServerWsUrl("ws://192.168.1.10:8080/ws/signaling");
        config.setHeartbeatIntervalSeconds(5);

        MachineIdProvider machineIdProvider = new MachineIdProvider();
        config.setMachineId(machineIdProvider.getOrCreateMachineId());

        return config;
    }
}