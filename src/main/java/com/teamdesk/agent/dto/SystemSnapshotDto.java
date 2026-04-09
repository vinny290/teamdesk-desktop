package com.teamdesk.agent.dto;

import java.util.List;

public class SystemSnapshotDto {

    private String os;
    private long uptimeSeconds;
    private String cpuModel;
    private double cpuLoad;
    private long totalMemory;
    private long availableMemory;
    private List<String> ipv4Addresses;

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public String getCpuModel() {
        return cpuModel;
    }

    public void setCpuModel(String cpuModel) {
        this.cpuModel = cpuModel;
    }

    public double getCpuLoad() {
        return cpuLoad;
    }

    public void setCpuLoad(double cpuLoad) {
        this.cpuLoad = cpuLoad;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getAvailableMemory() {
        return availableMemory;
    }

    public void setAvailableMemory(long availableMemory) {
        this.availableMemory = availableMemory;
    }

    public List<String> getIpv4Addresses() {
        return ipv4Addresses;
    }

    public void setIpv4Addresses(List<String> ipv4Addresses) {
        this.ipv4Addresses = ipv4Addresses;
    }
}