package com.teamdesk.agent.streaming;

public class AdaptiveStreamingModel {

    private static final double MAX_LATENCY_MS = 300.0;
    private static final double MAX_PACKET_LOSS_PERCENT = 10.0;
    private static final double MAX_CPU_PERCENT = 100.0;

    private static final double LATENCY_WEIGHT = 0.5;
    private static final double PACKET_LOSS_WEIGHT = 0.3;
    private static final double CPU_WEIGHT = 0.2;

    public AdaptiveStreamingDecision evaluate(
            double latencyMs,
            double packetLossPercent,
            double cpuLoadPercent
    ) {
        double normalizedLatency = clamp(latencyMs / MAX_LATENCY_MS);
        double normalizedPacketLoss = clamp(packetLossPercent / MAX_PACKET_LOSS_PERCENT);
        double normalizedCpu = clamp(cpuLoadPercent / MAX_CPU_PERCENT);

        double q = LATENCY_WEIGHT * normalizedLatency
                + PACKET_LOSS_WEIGHT * normalizedPacketLoss
                + CPU_WEIGHT * normalizedCpu;

        StreamingProfile profile;
        if (q < 0.35) {
            profile = StreamingProfile.HIGH;
        } else if (q < 0.70) {
            profile = StreamingProfile.MEDIUM;
        } else {
            profile = StreamingProfile.LOW;
        }

        return new AdaptiveStreamingDecision(
                round(q),
                round(latencyMs),
                round(packetLossPercent),
                round(cpuLoadPercent),
                profile
        );
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}