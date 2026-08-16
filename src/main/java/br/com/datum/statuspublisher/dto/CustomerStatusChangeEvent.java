package br.com.datum.statuspublisher.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Mensagem publicada na fila customer_status_changed - o mesmo formato
 * que o CustomerStatusChangeListener do datum-srv-clientes espera:
 * {
 *   "eventId": "cbca5352-22ad-48f2-aaf2-704735bc7737",
 *   "eventType": "CUSTOMER_STATUS_CHANGE",
 *   "customerId": 123,
 *   "status": "INACTIVE"
 * }
 *
 * Também devolvida como corpo da resposta HTTP, como confirmação do que
 * foi publicado.
 */
public class CustomerStatusChangeEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String eventId;
    private final String eventType;
    private final Long customerId;
    private final String status;

    public CustomerStatusChangeEvent(String eventId, String eventType, Long customerId, String status) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.customerId = customerId;
        this.status = status;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "CustomerStatusChangeEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", customerId=" + customerId +
                ", status='" + status + '\'' +
                '}';
    }
}
