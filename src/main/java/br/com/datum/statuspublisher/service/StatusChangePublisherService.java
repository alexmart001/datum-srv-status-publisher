package br.com.datum.statuspublisher.service;

import br.com.datum.statuspublisher.dto.CustomerStatusChangeEvent;
import br.com.datum.statuspublisher.util.StatusValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StatusChangePublisherService {

    private static final String EVENT_TYPE = "CUSTOMER_STATUS_CHANGE";

    private static final Logger logger = LoggerFactory.getLogger(StatusChangePublisherService.class);

    private final RabbitTemplate rabbitTemplate;
    private final String queueName;

    public StatusChangePublisherService(RabbitTemplate rabbitTemplate,
                                         @Value("${datum.rabbitmq.customer-status-change-queue}") String queueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.queueName = queueName;
    }

    /**
     * Monta e publica o evento CUSTOMER_STATUS_CHANGE. Diferente do
     * publisher do datum-srv-clientes (que é best-effort, pois o cliente
     * já foi persistido antes), aqui publicar É a própria operação - se a
     * publicação falhar, deixamos a exceção propagar para o controller
     * (mapeada para 502 pelo ApiExceptionHandler), já que não há nenhum
     * outro efeito colateral bem-sucedido para preservar.
     */
    public CustomerStatusChangeEvent publish(Long customerId, String status) {
        String normalizedStatus = StatusValidator.normalize(status);

        CustomerStatusChangeEvent event = new CustomerStatusChangeEvent(
                UUID.randomUUID().toString(), EVENT_TYPE, customerId, normalizedStatus);

        rabbitTemplate.convertAndSend(queueName, event);
        logger.info("Evento CUSTOMER_STATUS_CHANGE publicado na fila {}: {}", queueName, event);

        return event;
    }
}
