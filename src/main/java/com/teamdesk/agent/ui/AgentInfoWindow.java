package com.teamdesk.agent.ui;

import javax.swing.*;
import java.awt.*;

public class AgentInfoWindow {

    private final JFrame frame;
    private final JLabel machineIdLabel;
    private final JLabel serverLabel;

    public AgentInfoWindow(String machineId, String serverUrl) {
        frame = new JFrame("TeamDesk Agent");
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        frame.setSize(320, 140);
        frame.setLayout(new BorderLayout());
        frame.setAlwaysOnTop(false);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Агент запущен");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        machineIdLabel = new JLabel("Machine ID: " + machineId);
        serverLabel = new JLabel("Server: " + serverUrl);

        panel.add(title);
        panel.add(machineIdLabel);
        panel.add(serverLabel);

        frame.add(panel, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public void updateMachineId(String machineId) {
        SwingUtilities.invokeLater(() -> machineIdLabel.setText("Machine ID: " + machineId));
    }
}