package com.teamdesk.agent.transport;

import okhttp3.OkHttpClient;

import java.time.Duration;

public final class HttpClientFactory {

    private HttpClientFactory() {
    }

    public static OkHttpClient create() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .retryOnConnectionFailure(true)
                .build();
    }
}