package com.teamdesk.agent.streaming;

public class AdaptiveStreamingDecision {

    private final double score;
    private final double latencyMs;
    private final double packetLossPercent;
    private final double cpuLoadPercent;
    private final StreamingProfile profile;

    public AdaptiveStreamingDecision(
            double score,
            double latencyMs,
            double packetLossPercent,
            double cpuLoadPercent,
            StreamingProfile profile
    ) {
        this.score = score;
        this.latencyMs = latencyMs;
        this.packetLossPercent = packetLossPercent;
        this.cpuLoadPercent = cpuLoadPercent;
        this.profile = profile;
    }

    public double getScore() {
        return score;
    }

    public double getLatencyMs() {
        return latencyMs;
    }

    public double getPacketLossPercent() {
        return packetLossPercent;
    }

    public double getCpuLoadPercent() {
        return cpuLoadPercent;
    }

    public StreamingProfile getProfile() {
        return profile;
    }

    @Override
    public String toString() {
        return "AdaptiveStreamingDecision{" +
                "score=" + score +
                ", latencyMs=" + latencyMs +
                ", packetLossPercent=" + packetLossPercent +
                ", cpuLoadPercent=" + cpuLoadPercent +
                ", profile=" + profile +
                '}';
    }
}