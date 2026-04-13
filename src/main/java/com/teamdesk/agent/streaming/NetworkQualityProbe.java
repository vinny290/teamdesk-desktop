package com.teamdesk.agent.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;

public class NetworkQualityProbe {

    private static final Logger log = LoggerFactory.getLogger(NetworkQualityProbe.class);

    private static final int MAX_SAMPLES = 20;
    private static final int TIMEOUT_MS = 1500;

    private final String host;
    private final Deque<Boolean> successSamples = new ArrayDeque<>();
    private final Deque<Long> latencySamples = new ArrayDeque<>();

    public NetworkQualityProbe(String serverHttpBaseUrl) {
        try {
            URI uri = URI.create(serverHttpBaseUrl);
            this.host = uri.getHost();

            if (this.host == null || this.host.isBlank()) {
                throw new IllegalArgumentException("Cannot extract host from server url: " + serverHttpBaseUrl);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize NetworkQualityProbe", e);
        }
    }

    public synchronized void probe() {
        long startedAt = System.nanoTime();
        boolean success = false;
        long latencyMs = TIMEOUT_MS;

        try {
            InetAddress address = InetAddress.getByName(host);
            success = address.isReachable(TIMEOUT_MS);
            long elapsedNanos = System.nanoTime() - startedAt;
            latencyMs = Math.max(1, elapsedNanos / 1_000_000L);
        } catch (Exception e) {
            log.debug("Network probe failed: {}", e.getMessage());
        }

        addSuccessSample(success);
        if (success) {
            addLatencySample(latencyMs);
        }
    }

    public synchronized double getAverageLatencyMs() {
        if (latencySamples.isEmpty()) {
            return 300.0;
        }

        long sum = 0;
        for (Long value : latencySamples) {
            sum += value;
        }
        return (double) sum / latencySamples.size();
    }

    public synchronized double getPacketLossPercent() {
        if (successSamples.isEmpty()) {
            return 100.0;
        }

        int failed = 0;
        for (Boolean success : successSamples) {
            if (!success) {
                failed++;
            }
        }

        return ((double) failed / successSamples.size()) * 100.0;
    }

    private void addSuccessSample(boolean success) {
        if (successSamples.size() >= MAX_SAMPLES) {
            successSamples.removeFirst();
        }
        successSamples.addLast(success);
    }

    private void addLatencySample(long latencyMs) {
        if (latencySamples.size() >= MAX_SAMPLES) {
            latencySamples.removeFirst();
        }
        latencySamples.addLast(latencyMs);
    }
}