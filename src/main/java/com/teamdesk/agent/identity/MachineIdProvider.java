package com.teamdesk.agent.identity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MachineIdProvider {

    private static final String APP_DIR_NAME = ".teamdesk-agent";
    private static final String MACHINE_ID_FILE_NAME = "machine-id.txt";

    public String getOrCreateMachineId() {
        try {
            Path appDir = getAppDir();
            Path machineIdFile = appDir.resolve(MACHINE_ID_FILE_NAME);

            Files.createDirectories(appDir);

            if (Files.exists(machineIdFile)) {
                String existingId = Files.readString(machineIdFile, StandardCharsets.UTF_8).trim();

                if (MachineIdGenerator.isValidRawId(existingId)) {
                    return existingId;
                }

                throw new IllegalStateException(
                        "Invalid machineId format in file: " + machineIdFile + ". Expected exactly 10 digits."
                );
            }

            String newId = MachineIdGenerator.generateRawId();
            Files.writeString(machineIdFile, newId, StandardCharsets.UTF_8);

            return newId;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load or create machineId", e);
        }
    }

    private Path getAppDir() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            throw new IllegalStateException("user.home is not available");
        }

        return Path.of(userHome, APP_DIR_NAME);
    }
}