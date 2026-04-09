package com.teamdesk.agent.commands;

import com.teamdesk.agent.consent.ConsentService;

import javax.swing.*;

public class SafeCommandDispatcher {

    private final ConsentService consentService;

    public SafeCommandDispatcher(ConsentService consentService) {
        this.consentService = consentService;
    }

    public void showNotification(String text) {
        JOptionPane.showMessageDialog(null, text, "Уведомление", JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean requestRemoteSessionConsent(String requester) {
        return consentService.requestConsent("Пользователь " + requester + " запрашивает подключение. Разрешить?");
    }
}