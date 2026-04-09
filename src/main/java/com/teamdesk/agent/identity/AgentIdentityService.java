package com.teamdesk.agent.identity;

import com.teamdesk.agent.config.AgentConfig;

import java.net.InetAddress;

public class AgentIdentityService {

    private final AgentConfig config;

    public AgentIdentityService(AgentConfig config) {
        this.config = config;
    }

    public String getMachineId() {
        return config.getMachineId();
    }

    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }

    public String getOsName() {
        return System.getProperty("os.name");
    }

    public String getAgentVersion() {
        return "1.0-safe-agent";
    }

    public String getIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
}