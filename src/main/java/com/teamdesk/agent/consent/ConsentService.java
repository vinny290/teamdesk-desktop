package com.teamdesk.agent.consent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class ConsentService {

    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);

    public boolean requestConsent(String text) {
        if (GraphicsEnvironment.isHeadless()) {
            log.error("Cannot show consent dialog: headless environment");
            return false;
        }

        final int[] resultHolder = new int[1];

        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame frame = new JFrame("Запрос доступа");
                frame.setAlwaysOnTop(true);
                frame.setUndecorated(true);
                frame.setLocationRelativeTo(null);
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

                try {
                    frame.setVisible(true);
                    frame.toFront();
                    frame.requestFocus();

                    resultHolder[0] = JOptionPane.showConfirmDialog(
                            frame,
                            text,
                            "Запрос доступа",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );
                } finally {
                    frame.dispose();
                }
            });
        } catch (Exception e) {
            log.error("Failed to show consent dialog", e);
            return false;
        }

        boolean granted = resultHolder[0] == JOptionPane.YES_OPTION;
        log.info("Consent dialog result: granted={}", granted);
        return granted;
    }
}