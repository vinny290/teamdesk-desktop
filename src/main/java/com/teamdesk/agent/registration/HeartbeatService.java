package com.teamdesk.agent.registration;

import com.teamdesk.agent.config.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatService {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatService.class);

    private final AgentConfig config;
    private final RegistrationClient registrationClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public HeartbeatService(AgentConfig config, RegistrationClient registrationClient) {
        this.config = config;
        this.registrationClient = registrationClient;
    }

    public void start() {
        log.info("Starting heartbeat scheduler. interval={}s", config.getHeartbeatIntervalSeconds());

        scheduler.scheduleAtFixedRate(() -> {
            try {
                registrationClient.heartbeat();
            } catch (Exception e) {
                log.error("Heartbeat execution failed", e);
            }
        }, config.getHeartbeatIntervalSeconds(), config.getHeartbeatIntervalSeconds(), TimeUnit.SECONDS);
    }

    public void stop() {
        log.info("Stopping heartbeat scheduler");
        scheduler.shutdownNow();
    }
}