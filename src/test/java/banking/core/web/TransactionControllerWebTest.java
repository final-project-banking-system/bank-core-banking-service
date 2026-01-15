package banking.core.web;

import banking.core.controller.TransactionController;
import banking.core.dto.responses.TransactionResponse;
import banking.core.model.enums.TransactionStatus;
import banking.core.model.enums.TransactionType;
import banking.core.service.TransactionService;
import banking.core.service.publisher.SystemErrorPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class)
@Import(banking.core.config.SecurityConfig.class)
public class TransactionControllerWebTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    TransactionService transactionService;

    @MockitoBean
    SystemErrorPublisher systemErrorPublisher;

    @Test
    public void history_requiresAuth() throws Exception {
        mockMvc.perform(get("/transactions").param("accountId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void history_ok_returnsPage() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        var item = new TransactionResponse(UUID.randomUUID(), null, accountId, new BigDecimal("10.00"),
                TransactionType.DEPOSIT, TransactionStatus.COMPLETED, LocalDateTime.now());

        when(transactionService.getHistoryOfTransactions(eq(userId), eq(accountId), any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/transactions")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .param("accountId", accountId.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].toAccountId").value(accountId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
