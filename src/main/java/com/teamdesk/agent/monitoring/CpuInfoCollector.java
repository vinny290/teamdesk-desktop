package com.teamdesk.agent.monitoring;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class CpuInfoCollector {

    private final SystemInfo systemInfo = new SystemInfo();

    public String getModel() {
        CentralProcessor cpu = systemInfo.getHardware().getProcessor();
        return cpu.getProcessorIdentifier().getName();
    }

    private long[] previousTicks;

    public double getLoad() {
        CentralProcessor cpu = systemInfo.getHardware().getProcessor();

        if (previousTicks == null) {
            previousTicks = cpu.getSystemCpuLoadTicks();
            return 0.0;
        }

        double load = cpu.getSystemCpuLoadBetweenTicks(previousTicks);
        previousTicks = cpu.getSystemCpuLoadTicks();

        return load;
    }
}