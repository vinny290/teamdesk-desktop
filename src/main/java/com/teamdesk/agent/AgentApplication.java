package com.teamdesk.agent;

import com.teamdesk.agent.config.AgentConfig;
import com.teamdesk.agent.config.ConfigLoader;
import com.teamdesk.agent.consent.ConsentService;
import com.teamdesk.agent.identity.AgentIdentityService;
import com.teamdesk.agent.input.RemoteInputService;
import com.teamdesk.agent.monitoring.SystemSnapshotService;
import com.teamdesk.agent.registration.HeartbeatService;
import com.teamdesk.agent.registration.RegistrationClient;
import com.teamdesk.agent.session.AgentSessionState;
import com.teamdesk.agent.transport.AgentWebSocketClient;
import com.teamdesk.agent.ui.AgentInfoWindow;
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
            AgentSessionState sessionState = new AgentSessionState();
            RemoteInputService remoteInputService = new RemoteInputService();

            RegistrationClient registrationClient =
                    new RegistrationClient(config, identityService, snapshotService);
            registrationClient.register();

            AgentWebSocketClient webSocketClient =
                    new AgentWebSocketClient(config, consentService, sessionState, remoteInputService);
            webSocketClient.connect();

            HeartbeatService heartbeatService =
                    new HeartbeatService(config, registrationClient);
            heartbeatService.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown hook triggered");
                heartbeatService.stop();
                webSocketClient.close();
            }));

            AgentInfoWindow infoWindow = new AgentInfoWindow(
                    config.getMachineId(),
                    config.getServerHttpBaseUrl()
            );
            infoWindow.show();

            log.info("Desktop agent started successfully. machineId={}", identityService.getMachineId());
        } catch (Exception e) {
            log.error("Agent startup failed", e);
        }
    }
}