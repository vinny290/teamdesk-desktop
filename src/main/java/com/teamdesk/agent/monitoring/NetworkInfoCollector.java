package com.teamdesk.agent.monitoring;

import oshi.SystemInfo;
import oshi.hardware.NetworkIF;

import java.util.ArrayList;
import java.util.List;

public class NetworkInfoCollector {

    private final SystemInfo systemInfo = new SystemInfo();

    public List<String> getIpv4Addresses() {
        List<String> result = new ArrayList<>();

        for (NetworkIF net : systemInfo.getHardware().getNetworkIFs()) {
            net.updateAttributes();
            for (String ip : net.getIPv4addr()) {
                result.add(ip);
            }
        }

        return result;
    }
}