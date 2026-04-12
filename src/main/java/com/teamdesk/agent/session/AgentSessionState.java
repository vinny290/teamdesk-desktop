package com.teamdesk.agent.session;

import java.util.concurrent.atomic.AtomicBoolean;

public class AgentSessionState {

    private volatile String sessionId;
    private volatile String viewerId;
    private volatile String machineId;
    private final AtomicBoolean consentGranted = new AtomicBoolean(false);
    private final AtomicBoolean active = new AtomicBoolean(false);

    public String getSessionId() {
        return sessionId;
    }

    public String getViewerId() {
        return viewerId;
    }

    public String getMachineId() {
        return machineId;
    }

    public boolean isConsentGranted() {
        return consentGranted.get();
    }

    public boolean isActive() {
        return active.get();
    }

    public synchronized void openPendingSession(String sessionId, String viewerId, String machineId) {
        this.sessionId = sessionId;
        this.viewerId = viewerId;
        this.machineId = machineId;
        this.consentGranted.set(false);
        this.active.set(false);
    }

    public synchronized void markConsentGranted() {
        this.consentGranted.set(true);
    }

    public synchronized void markConsentDeclined() {
        this.consentGranted.set(false);
        this.active.set(false);
    }

    public synchronized void markActive() {
        this.active.set(true);
    }

    public synchronized void clear() {
        this.sessionId = null;
        this.viewerId = null;
        this.machineId = null;
        this.consentGranted.set(false);
        this.active.set(false);
    }

    public synchronized boolean matches(String sessionId, String viewerId, String machineId) {
        return equalsNullable(this.sessionId, sessionId)
                && equalsNullable(this.viewerId, viewerId)
                && equalsNullable(this.machineId, machineId);
    }

    private boolean equalsNullable(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    @Override
    public synchronized String toString() {
        return "AgentSessionState{" +
                "sessionId='" + sessionId + '\'' +
                ", viewerId='" + viewerId + '\'' +
                ", machineId='" + machineId + '\'' +
                ", consentGranted=" + consentGranted.get() +
                ", active=" + active.get() +
                '}';
    }
}