package com.teamdesk.agent.consent;

import javax.swing.*;

public class ConsentService {

    public boolean requestConsent(String text) {
        int result = JOptionPane.showConfirmDialog(
                null,
                text,
                "Запрос доступа",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        return result == JOptionPane.YES_OPTION;
    }
}