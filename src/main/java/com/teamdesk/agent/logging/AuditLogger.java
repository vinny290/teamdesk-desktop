package com.teamdesk.agent.logging;

public class AuditLogger {

    public void info(String message) {
        System.out.println("[AUDIT] " + message);
    }
}