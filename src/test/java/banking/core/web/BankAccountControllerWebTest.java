package banking.core.web;

import banking.core.controller.BankAccountController;
import banking.core.dto.requests.CreateBankAccountRequest;
import banking.core.dto.responses.BankAccountResponse;
import banking.core.model.enums.AccountStatus;
import banking.core.model.enums.Currency;
import banking.core.service.BankAccountService;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BankAccountController.class)
@Import(banking.core.config.SecurityConfig.class)
public class BankAccountControllerWebTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    BankAccountService bankAccountService;

    @MockitoBean
    SystemErrorPublisher systemErrorPublisher;

    @Test
    void createAccount_requiresAuth() throws Exception {
        var body = new CreateBankAccountRequest(Currency.EUR);

        mockMvc.perform(post("/accounts").contentType("application/json")
                .content(objectMapper.writeValueAsString(body))).andExpect(status().isUnauthorized());
    }

    @Test
    void createAccount_ok_returns201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        var body = new CreateBankAccountRequest(Currency.EUR);

        var response = new BankAccountResponse(accountId, userId, "ACC-123", new BigDecimal("0.00"),
                Currency.EUR, AccountStatus.ACTIVE, LocalDateTime.now());

        when(bankAccountService.createBankAccount(eq(userId), any(CreateBankAccountRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/accounts")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }
}
