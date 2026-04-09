package com.teamdesk.agent.monitoring;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

public class MemoryInfoCollector {

    private final SystemInfo systemInfo = new SystemInfo();

    public long getTotalMemory() {
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        return memory.getTotal();
    }

    public long getAvailableMemory() {
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        return memory.getAvailable();
    }
}