package com.teamdesk.agent.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamdesk.agent.config.AgentConfig;
import com.teamdesk.agent.consent.ConsentService;
import com.teamdesk.agent.dto.AgentSignalEnvelope;
import com.teamdesk.agent.session.AgentSessionState;
import com.teamdesk.agent.util.JsonMapperFactory;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentWebSocketClient extends WebSocketListener {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketClient.class);

    private final AgentConfig config;
    private final ConsentService consentService;
    private final AgentSessionState sessionState;
    private final ObjectMapper objectMapper = JsonMapperFactory.create();
    private final OkHttpClient httpClient = HttpClientFactory.create();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();

    private volatile WebSocket webSocket;
    private volatile boolean manualClose = false;
    private volatile boolean reconnectScheduled = false;

    public AgentWebSocketClient(
            AgentConfig config,
            ConsentService consentService,
            AgentSessionState sessionState
    ) {
        this.config = config;
        this.consentService = consentService;
        this.sessionState = sessionState;
    }

    public synchronized void connect() {
        manualClose = false;

        Request request = new Request.Builder()
                .url(config.getServerWsUrl())
                .build();

        log.info("Connecting to signaling server: {}", config.getServerWsUrl());
        this.webSocket = httpClient.newWebSocket(request, this);
    }

    public synchronized void close() {
        manualClose = true;

        if (webSocket != null) {
            log.info("Closing WebSocket connection");
            webSocket.close(1000, "Agent shutdown");
        }

        reconnectExecutor.shutdownNow();
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        reconnectScheduled = false;
        log.info("WebSocket connected");

        try {
            AgentSignalEnvelope envelope = new AgentSignalEnvelope();
            envelope.setType("REGISTER_AGENT");
            envelope.setMachineId(config.getMachineId());

            String json = objectMapper.writeValueAsString(envelope);
            webSocket.send(json);

            log.info("REGISTER_AGENT sent. machineId={}", config.getMachineId());
        } catch (Exception e) {
            log.error("Failed to send REGISTER_AGENT", e);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        log.debug("WS message received: {}", text);

        try {
            AgentSignalEnvelope envelope = objectMapper.readValue(text, AgentSignalEnvelope.class);
            handleMessage(webSocket, envelope);
        } catch (Exception e) {
            log.error("Failed to process WS message: {}", text, e);
        }
    }

    private void handleMessage(WebSocket webSocket, AgentSignalEnvelope envelope) throws Exception {
        String type = envelope.getType();

        if ("REQUEST_CONSENT".equals(type)) {
            handleConsentRequest(webSocket, envelope);
            return;
        }

        log.warn("Unsupported WS message type received: {}", type);
    }

    private void handleConsentRequest(WebSocket webSocket, AgentSignalEnvelope envelope) throws Exception {
        if (isBlank(envelope.getViewerId()) || isBlank(envelope.getMachineId())) {
            log.warn("Invalid REQUEST_CONSENT received: {}", objectMapper.writeValueAsString(envelope));
            return;
        }

        log.info("REQUEST_CONSENT received. viewerId={}, machineId={}, sessionId={}",
                envelope.getViewerId(), envelope.getMachineId(), envelope.getSessionId());

        sessionState.openPendingSession(
                envelope.getSessionId(),
                envelope.getViewerId(),
                envelope.getMachineId()
        );

        boolean granted = consentService.requestConsent(
                "Разрешить удалённое подключение для viewer: " + envelope.getViewerId() + "?"
        );

        AgentSignalEnvelope response = new AgentSignalEnvelope();
        response.setType(granted ? "CONSENT_GRANTED" : "CONSENT_DECLINED");
        response.setSessionId(envelope.getSessionId());
        response.setViewerId(envelope.getViewerId());
        response.setMachineId(envelope.getMachineId());

        if (granted) {
            sessionState.markConsentGranted();
            log.info("Consent granted. state={}", sessionState);
        } else {
            sessionState.markConsentDeclined();
            log.info("Consent declined. state={}", sessionState);
        }

        webSocket.send(objectMapper.writeValueAsString(response));
        log.info("Consent response sent: {}", response.getType());

        if (!granted) {
            sessionState.clear();
        }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        log.warn("WebSocket closing. code={}, reason={}", code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        log.warn("WebSocket closed. code={}, reason={}", code, reason);
        scheduleReconnectIfNeeded();
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        if (response != null) {
            log.error("WebSocket failure. code={}, message={}", response.code(), response.message(), t);
        } else {
            log.error("WebSocket failure without HTTP response", t);
        }

        scheduleReconnectIfNeeded();
    }

    private synchronized void scheduleReconnectIfNeeded() {
        if (manualClose) {
            log.info("Reconnect skipped because close was manual");
            return;
        }

        if (reconnectScheduled) {
            return;
        }

        reconnectScheduled = true;

        log.warn("Scheduling reconnect in 3 seconds");
        reconnectExecutor.schedule(() -> {
            try {
                reconnectScheduled = false;
                connect();
            } catch (Exception e) {
                log.error("Reconnect attempt failed", e);
                scheduleReconnectIfNeeded();
            }
        }, 3, TimeUnit.SECONDS);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}