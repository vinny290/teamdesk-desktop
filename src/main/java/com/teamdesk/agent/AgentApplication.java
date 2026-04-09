package com.teamdesk.agent;

import com.teamdesk.agent.config.AgentConfig;
import com.teamdesk.agent.config.ConfigLoader;
import com.teamdesk.agent.consent.ConsentService;
import com.teamdesk.agent.identity.AgentIdentityService;
import com.teamdesk.agent.monitoring.SystemSnapshotService;
import com.teamdesk.agent.registration.HeartbeatService;
import com.teamdesk.agent.registration.RegistrationClient;
import com.teamdesk.agent.transport.AgentWebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentApplication {

    private static final Logger log = LoggerFactory.getLogger(AgentApplication.class);

    public static void main(String[] args) {
        try {
            AgentConfig config = ConfigLoader.load();

            AgentIdentityService identityService = new AgentIdentityService(config);
            ConsentService consentService = new ConsentService();
            SystemSnapshotService snapshotService = new SystemSnapshotService();

            RegistrationClient registrationClient = new RegistrationClient(config, identityService, snapshotService);
            registrationClient.register();

            AgentWebSocketClient webSocketClient =
                    new AgentWebSocketClient(config, consentService);
            webSocketClient.connect();

            HeartbeatService heartbeatService = new HeartbeatService(config, registrationClient);
            heartbeatService.start();

            log.info("Desktop agent started successfully. machineId={}", identityService.getMachineId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}