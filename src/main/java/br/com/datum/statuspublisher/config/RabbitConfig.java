package br.com.datum.statuspublisher.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara a mesma fila customer_status_changed que o datum-srv-clientes
 * consome (mesmas propriedades: durável, não-exclusiva, tipo classic) -
 * declaração idempotente, então funciona independente de qual dos dois
 * serviços sobe primeiro.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public Queue customerStatusChangeQueue(@Value("${datum.rabbitmq.customer-status-change-queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }
}
