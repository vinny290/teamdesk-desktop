package com.teamdesk.agent.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamdesk.agent.config.AgentConfig;
import com.teamdesk.agent.consent.ConsentService;
import com.teamdesk.agent.dto.AgentSignalEnvelope;
import com.teamdesk.agent.util.JsonMapperFactory;
import okhttp3.*;

public class AgentWebSocketClient extends WebSocketListener {

    private final AgentConfig config;
    private final ConsentService consentService;
    private final ObjectMapper objectMapper = JsonMapperFactory.create();

    private WebSocket webSocket;

    public AgentWebSocketClient(AgentConfig config, ConsentService consentService) {
        this.config = config;
        this.consentService = consentService;
    }

    public void connect() {
        OkHttpClient client = HttpClientFactory.create();

        Request request = new Request.Builder()
                .url(config.getServerWsUrl())
                .build();

        this.webSocket = client.newWebSocket(request, this);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        System.out.println("WebSocket connected");

        try {
            AgentSignalEnvelope envelope = new AgentSignalEnvelope();
            envelope.setType("REGISTER_AGENT");
            envelope.setMachineId(config.getMachineId());

            String json = objectMapper.writeValueAsString(envelope);
            webSocket.send(json);

            System.out.println("REGISTER_AGENT sent: " + config.getMachineId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        System.out.println("WS message: " + text);

        try {
            AgentSignalEnvelope envelope = objectMapper.readValue(text, AgentSignalEnvelope.class);

            if ("REQUEST_CONSENT".equals(envelope.getType())) {

                System.out.println("Получен REQUEST_CONSENT");

                boolean granted = consentService.requestConsent(
                        "Разрешить удалённое подключение для viewer: " + envelope.getViewerId() + "?"
                );

                AgentSignalEnvelope response = new AgentSignalEnvelope();
                response.setType(granted ? "CONSENT_GRANTED" : "CONSENT_DECLINED");
                response.setMachineId(envelope.getMachineId());
                response.setViewerId(envelope.getViewerId());
                response.setSessionId(envelope.getSessionId());

                webSocket.send(objectMapper.writeValueAsString(response));

                System.out.println("CONSENT response sent: " + response.getType());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        t.printStackTrace();
    }
}