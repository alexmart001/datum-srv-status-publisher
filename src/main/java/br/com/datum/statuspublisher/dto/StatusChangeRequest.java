package br.com.datum.statuspublisher.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo esperado em POST /customers/{id}/status:
 * { "status": "INACTIVE" }
 */
public class StatusChangeRequest {

    @NotBlank(message = "status é obrigatório")
    private String status;

    public StatusChangeRequest() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
