package br.com.datum.statuspublisher.util;

/**
 * Valida e normaliza o valor de status recebido, garantindo que só
 * "ACTIVE"/"INACTIVE" (case-insensitive) sejam publicados na fila - o
 * mesmo contrato que o CustomerStatusChangeListener do datum-srv-clientes
 * espera.
 */
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
