package com.teamdesk.agent.transport;

import okhttp3.OkHttpClient;

public class HttpClientFactory {

    public static OkHttpClient create() {
        return new OkHttpClient.Builder().build();
    }
}