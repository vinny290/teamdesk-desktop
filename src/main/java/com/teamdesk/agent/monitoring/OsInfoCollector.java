package com.teamdesk.agent.monitoring;

import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;

public class OsInfoCollector {

    private final SystemInfo systemInfo = new SystemInfo();

    public String getOs() {
        OperatingSystem os = systemInfo.getOperatingSystem();
        return os.toString();
    }

    public long getUptimeSeconds() {
        return systemInfo.getOperatingSystem().getSystemUptime();
    }
}