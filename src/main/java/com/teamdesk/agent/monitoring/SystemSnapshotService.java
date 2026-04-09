package com.teamdesk.agent.monitoring;

import com.teamdesk.agent.dto.SystemSnapshotDto;

public class SystemSnapshotService {

    private final OsInfoCollector osInfoCollector = new OsInfoCollector();
    private final CpuInfoCollector cpuInfoCollector = new CpuInfoCollector();
    private final MemoryInfoCollector memoryInfoCollector = new MemoryInfoCollector();
    private final NetworkInfoCollector networkInfoCollector = new NetworkInfoCollector();

    public SystemSnapshotDto collect() {
        SystemSnapshotDto dto = new SystemSnapshotDto();
        dto.setOs(osInfoCollector.getOs());
        dto.setUptimeSeconds(osInfoCollector.getUptimeSeconds());
        dto.setCpuModel(cpuInfoCollector.getModel());
        dto.setCpuLoad(cpuInfoCollector.getLoad());
        dto.setTotalMemory(memoryInfoCollector.getTotalMemory());
        dto.setAvailableMemory(memoryInfoCollector.getAvailableMemory());
        dto.setIpv4Addresses(networkInfoCollector.getIpv4Addresses());
        return dto;
    }
}