package com.teamdesk.agent.ui;

import com.teamdesk.agent.identity.MachineIdGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class AgentInfoWindow {

    private final JFrame frame;
    private final JLabel machineIdLabel;
    private final JLabel serverLabel;
    private final JLabel viewerStatusLabel;
    private final JButton copyIdButton;

    public AgentInfoWindow(String rawMachineId, String serverUrl) {
        frame = new JFrame("TeamDesk Agent");
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        frame.setSize(380, 190);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Агент запущен");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        machineIdLabel = new JLabel("Machine ID: " + MachineIdGenerator.formatId(rawMachineId));
        serverLabel = new JLabel("Server: " + serverUrl);
        viewerStatusLabel = new JLabel("Статус: viewer не подключен");

        copyIdButton = new JButton("Скопировать ID");
        copyIdButton.addActionListener(e -> copyToClipboard(rawMachineId));

        panel.add(title);
        panel.add(machineIdLabel);
        panel.add(serverLabel);
        panel.add(viewerStatusLabel);
        panel.add(copyIdButton);

        frame.add(panel, BorderLayout.CENTER);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public void updateMachineId(String rawMachineId) {
        SwingUtilities.invokeLater(() ->
                machineIdLabel.setText("Machine ID: " + MachineIdGenerator.formatId(rawMachineId))
        );
    }

    public void setViewerConnected(boolean connected, String viewerId) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                if (viewerId != null && !viewerId.isBlank()) {
                    viewerStatusLabel.setText("Статус: viewer подключен (" + viewerId + ")");
                } else {
                    viewerStatusLabel.setText("Статус: viewer подключен");
                }
            } else {
                viewerStatusLabel.setText("Статус: viewer не подключен");
            }
        });
    }

    private void copyToClipboard(String rawMachineId) {
        StringSelection selection = new StringSelection(rawMachineId);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }
}