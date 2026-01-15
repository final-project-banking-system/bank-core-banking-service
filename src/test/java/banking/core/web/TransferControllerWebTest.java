package banking.core.web;

import banking.core.controller.TransferController;
import banking.core.dto.requests.TransferRequest;
import banking.core.dto.responses.TransferResponse;
import banking.core.service.TransferService;
import banking.core.service.publisher.SystemErrorPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransferController.class)
@Import(banking.core.config.SecurityConfig.class)
public class TransferControllerWebTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    TransferService transferService;

    @MockitoBean
    SystemErrorPublisher systemErrorPublisher;

    @Test
    public void transfer_requiresAuth() throws Exception {
        var request = new TransferRequest(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"));

        mockMvc.perform(post("/transfers").contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))).andExpect(status().isUnauthorized());
    }

    @Test
    public void transfer_invalidBody_returns400() throws Exception {
        UUID userId = UUID.randomUUID();

        var request = new TransferRequest(UUID.randomUUID(), UUID.randomUUID(), null);

        mockMvc.perform(post("/transfers")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transfer_ok_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        var request = new TransferRequest(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"));

        when(transferService.transfer(eq(userId), any(TransferRequest.class)))
                .thenReturn(new TransferResponse(txId));

        mockMvc.perform(post("/transfers")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId.toString()));
    }
}
