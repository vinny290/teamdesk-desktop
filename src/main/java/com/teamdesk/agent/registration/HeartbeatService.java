package com.teamdesk.agent.registration;

import com.teamdesk.agent.config.AgentConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatService {

    private final AgentConfig config;
    private final RegistrationClient registrationClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public HeartbeatService(AgentConfig config, RegistrationClient registrationClient) {
        this.config = config;
        this.registrationClient = registrationClient;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                registrationClient.heartbeat();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, config.getHeartbeatIntervalSeconds(), config.getHeartbeatIntervalSeconds(), TimeUnit.SECONDS);
    }
}