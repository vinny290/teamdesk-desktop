package com.teamdesk.agent.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamdesk.agent.config.AgentConfig;
import com.teamdesk.agent.dto.AgentHeartbeatRequest;
import com.teamdesk.agent.dto.AgentRegisterRequest;
import com.teamdesk.agent.identity.AgentIdentityService;
import com.teamdesk.agent.monitoring.SystemSnapshotService;
import com.teamdesk.agent.transport.HttpClientFactory;
import com.teamdesk.agent.util.JsonMapperFactory;
import okhttp3.*;

public class RegistrationClient {

    private final AgentConfig config;
    private final AgentIdentityService identityService;
    private final SystemSnapshotService snapshotService;
    private final OkHttpClient httpClient = HttpClientFactory.create();
    private final ObjectMapper objectMapper = JsonMapperFactory.create();

    public RegistrationClient(
            AgentConfig config,
            AgentIdentityService identityService,
            SystemSnapshotService snapshotService
    ) {
        this.config = config;
        this.identityService = identityService;
        this.snapshotService = snapshotService;
    }

    public void register() throws Exception {
        AgentRegisterRequest requestDto = new AgentRegisterRequest();
        requestDto.setMachineId(identityService.getMachineId());
        requestDto.setHostname(identityService.getHostname());
        requestDto.setOsName(identityService.getOsName());
        requestDto.setAgentVersion(identityService.getAgentVersion());
        requestDto.setIp(identityService.getIp());

        String json = objectMapper.writeValueAsString(requestDto);

        Request request = new Request.Builder()
                .url(config.getServerHttpBaseUrl() + "/api/machines/register")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Register failed: " + response.code());
            }
        }

        System.out.println("Machine registered");
        System.out.println("Snapshot: " + objectMapper.writeValueAsString(snapshotService.collect()));
    }

    public void heartbeat() throws Exception {
        AgentHeartbeatRequest dto = new AgentHeartbeatRequest();
        dto.setMachineId(identityService.getMachineId());
        dto.setIp(identityService.getIp());

        String json = objectMapper.writeValueAsString(dto);

        Request request = new Request.Builder()
                .url(config.getServerHttpBaseUrl() + "/api/machines/heartbeat")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Heartbeat failed: " + response.code());
            }
        }

        System.out.println("Heartbeat sent");
    }
}