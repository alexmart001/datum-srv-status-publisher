package br.com.datum.statuspublisher.util;

public final class StatusValidator {

    private StatusValidator() {}

    public static String normalize(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Status não informado. Valores aceitos: ACTIVE, INACTIVE.");
        }

        String normalized = status.trim().toUpperCase();
        if (!normalized.equals("ACTIVE") && !normalized.equals("INACTIVE")) {
            throw new IllegalArgumentException(
                    "Status inválido: '" + status + "'. Valores aceitos: ACTIVE, INACTIVE.");
        }

        return normalized;
    }
}
