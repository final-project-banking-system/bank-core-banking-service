package banking.core.dto.responses;

import banking.core.model.enums.AccountStatus;
import banking.core.model.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountResponse {
    private UUID id;
    private UUID userId;
    private String accountNumber;
    private BigDecimal balance;
    private Currency currency;
    private AccountStatus status;
    private LocalDateTime createdAt;
}
