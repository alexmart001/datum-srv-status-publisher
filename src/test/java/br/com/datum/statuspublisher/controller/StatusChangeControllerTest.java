package br.com.datum.statuspublisher.controller;

import br.com.datum.statuspublisher.config.SecurityConfig;
import br.com.datum.statuspublisher.dto.CustomerStatusChangeEvent;
import br.com.datum.statuspublisher.service.StatusChangePublisherService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.ConnectException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes do endpoint POST /customers/{id}/status, usando MockMvc + o
 * StatusChangePublisherService mockado - sem tocar em RabbitMQ real. O
 * JwtDecoder também é mockado para o contexto de teste nunca tentar
 * resolver o issuer-uri real via rede.
 */
@WebMvcTest(StatusChangeController.class)
@Import(SecurityConfig.class)
class StatusChangeControllerTest {

    private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
    private static final SimpleGrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatusChangePublisherService statusChangePublisherService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void changeStatus_semToken_retorna401() throws Exception {
        mockMvc.perform(post("/customers/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(statusChangePublisherService);
    }

    @Test
    void changeStatus_comTokenUser_retorna403() throws Exception {
        mockMvc.perform(post("/customers/1/status")
                        .with(jwt().authorities(ROLE_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(statusChangePublisherService);
    }

    @Test
    void changeStatus_comTokenAdminEStatusValido_retorna202ComEventoPublicado() throws Exception {
        CustomerStatusChangeEvent event = new CustomerStatusChangeEvent(
                "cbca5352-22ad-48f2-aaf2-704735bc7737", "CUSTOMER_STATUS_CHANGE", 1L, "INACTIVE");
        when(statusChangePublisherService.publish(1L, "INACTIVE")).thenReturn(event);

        mockMvc.perform(post("/customers/1/status")
                        .with(jwt().authorities(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("cbca5352-22ad-48f2-aaf2-704735bc7737"))
                .andExpect(jsonPath("$.eventType").value("CUSTOMER_STATUS_CHANGE"))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(statusChangePublisherService).publish(1L, "INACTIVE");
    }

    @Test
    void changeStatus_statusAusenteNoBody_retorna400SemChamarService() throws Exception {
        mockMvc.perform(post("/customers/1/status")
                        .with(jwt().authorities(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(statusChangePublisherService);
    }

    @Test
    void changeStatus_statusEmBrancoNoBody_retorna400() throws Exception {
        mockMvc.perform(post("/customers/1/status")
                        .with(jwt().authorities(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(statusChangePublisherService);
    }

    @Test
    void changeStatus_statusComValorInvalido_retorna400ComMensagemDoServico() throws Exception {
        when(statusChangePublisherService.publish(eq(1L), eq("FOO")))
                .thenThrow(new IllegalArgumentException("Status inválido: 'FOO'. Valores aceitos: ACTIVE, INACTIVE."));

        mockMvc.perform(post("/customers/1/status")
                        .with(jwt().authorities(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FOO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Status inválido: 'FOO'. Valores aceitos: ACTIVE, INACTIVE."));
    }

    @Test
    void changeStatus_falhaAoPublicarNoRabbit_retorna502() throws Exception {
        when(statusChangePublisherService.publish(1L, "ACTIVE"))
                .thenThrow(new AmqpConnectException(new ConnectException("Connection refused")));

        mockMvc.perform(post("/customers/1/status")
                        .with(jwt().authorities(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void changeStatus_corpoAusente_retorna400() throws Exception {
        mockMvc.perform(post("/customers/1/status")
                        .with(jwt().authorities(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(statusChangePublisherService, never()).publish(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }
}
