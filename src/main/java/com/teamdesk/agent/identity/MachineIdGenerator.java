package com.teamdesk.agent.identity;

import java.security.SecureRandom;

public final class MachineIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private MachineIdGenerator() {
    }

    /**
     * Генерирует сырой machineId из 10 цифр без пробелов.
     * Пример: 4123456789
     */
    public static String generateRawId() {
        StringBuilder digits = new StringBuilder(10);

        for (int i = 0; i < 10; i++) {
            digits.append(RANDOM.nextInt(10));
        }

        return digits.toString();
    }

    /**
     * Форматирует сырой ID в вид: X XXX XXX XXX
     * Пример: 4123456789 -> 4 123 456 789
     */
    public static String formatId(String rawId) {
        if (rawId == null) {
            throw new IllegalArgumentException("rawId must not be null");
        }

        String normalized = rawId.replaceAll("\\s+", "");

        if (!normalized.matches("\\d{10}")) {
            throw new IllegalArgumentException("rawId must contain exactly 10 digits");
        }

        return normalized.substring(0, 1) + " "
                + normalized.substring(1, 4) + " "
                + normalized.substring(4, 7) + " "
                + normalized.substring(7, 10);
    }

    /**
     * Проверяет, что ID состоит ровно из 10 цифр без пробелов.
     */
    public static boolean isValidRawId(String rawId) {
        return rawId != null && rawId.matches("\\d{10}");
    }
}