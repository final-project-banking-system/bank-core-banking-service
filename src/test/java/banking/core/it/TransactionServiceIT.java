package banking.core.it;

import banking.core.dto.requests.TransactionFilter;
import banking.core.dto.responses.TransactionResponse;
import banking.core.error.exception.TransferBusinessException;
import banking.core.model.entity.BankAccount;
import banking.core.model.entity.Transaction;
import banking.core.model.enums.AccountStatus;
import banking.core.model.enums.Currency;
import banking.core.model.enums.TransactionStatus;
import banking.core.model.enums.TransactionType;
import banking.core.repository.BankAccountRepository;
import banking.core.repository.TransactionRepository;
import banking.core.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TransactionServiceIT extends IntegrationTestBase {
    @Autowired
    TransactionService transactionService;

    @Autowired
    BankAccountRepository bankAccountRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @BeforeEach
    void cleanDb() {
        transactionRepository.deleteAll();
        bankAccountRepository.deleteAll();
    }

    @Test
    public void getHistory_returnsTransactionsForAccount() {
        UUID userId = UUID.randomUUID();

        BankAccount bankAccount = bankAccountRepository.save(BankAccount.builder()
                .userId(userId)
                .accountNumber("ACC-HIST-" + System.currentTimeMillis())
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("0.00"))
                .build());

        Transaction transaction1 = transactionRepository.save(Transaction.builder()
                .fromAccount(null)
                .toAccount(bankAccount)
                .amount(new BigDecimal("10.00"))
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .build());

        Transaction transaction2 = transactionRepository.save(Transaction.builder()
                .fromAccount(bankAccount)
                .toAccount(null)
                .amount(new BigDecimal("5.00"))
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .build());

        TransactionFilter filter = new TransactionFilter();
        filter.setAccountId(bankAccount.getId());

        var page = transactionService.getHistoryOfTransactions(filter, userId, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());

        var ids = page.getContent().stream().map(TransactionResponse::getId).toList();
        assertTrue(ids.contains(transaction1.getId()));
        assertTrue(ids.contains(transaction2.getId()));
    }

    @Test
    public void getHistory_whenAccountNotBelongsToUser_throws() {
        UUID userId = UUID.randomUUID();

        BankAccount bankAccount = bankAccountRepository.save(BankAccount.builder()
                .userId(UUID.randomUUID())
                .accountNumber("ACC-FOREIGN-" + System.currentTimeMillis())
                .currency(Currency.EUR)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("0.00"))
                .build());

        TransactionFilter filter = new TransactionFilter();
        filter.setAccountId(bankAccount.getId());

        var exception = assertThrows(TransferBusinessException.class,
                () -> transactionService.getHistoryOfTransactions(filter, userId, PageRequest.of(0, 10)));

        assertNotNull(exception.getMessage());
    }
}
