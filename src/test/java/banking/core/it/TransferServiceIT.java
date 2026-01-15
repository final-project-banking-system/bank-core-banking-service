package banking.core.it;

import banking.core.dto.requests.TransferRequest;
import banking.core.model.entity.BankAccount;
import banking.core.model.enums.AccountStatus;
import banking.core.model.enums.Currency;
import banking.core.repository.BankAccountRepository;
import banking.core.repository.OutboxEventRepository;
import banking.core.repository.TransactionRepository;
import banking.core.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TransferServiceIT extends IntegrationTestBase {
    @Autowired
    TransferService transferService;
    @Autowired
    BankAccountRepository bankAccountRepository;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDb() {
        transactionRepository.deleteAll();
        outboxEventRepository.deleteAll();
        bankAccountRepository.deleteAll();
    }

    @Test
    void transfer_movesMoney_createsTransaction_andOutboxEvent() {
        UUID userId = UUID.randomUUID();

        BankAccount from = bankAccountRepository.save(BankAccount.builder()
                .userId(userId)
                .accountNumber("ACC-FROM-" + System.currentTimeMillis())
                .currency(Currency.EUR)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .build());

        BankAccount to = bankAccountRepository.save(BankAccount.builder()
                .userId(UUID.randomUUID())
                .accountNumber("ACC-TO-" + System.currentTimeMillis())
                .currency(Currency.EUR)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("10.00"))
                .build());

        long outboxBefore = outboxEventRepository.count();
        long txBefore = transactionRepository.count();

        var response = transferService.transfer(userId, new TransferRequest(
                from.getId(),
                to.getId(),
                new BigDecimal("25.00")
        ));

        assertNotNull(response.getTransactionId());

        var fromAfter = bankAccountRepository.findById(from.getId()).orElseThrow();
        var toAfter = bankAccountRepository.findById(to.getId()).orElseThrow();

        assertEquals(0, fromAfter.getBalance().compareTo(new BigDecimal("75.00")));
        assertEquals(0, toAfter.getBalance().compareTo(new BigDecimal("35.00")));

        var transaction = transactionRepository.findById(response.getTransactionId()).orElseThrow();
        assertEquals(from.getId(), transaction.getFromAccount().getId());
        assertEquals(to.getId(), transaction.getToAccount().getId());
        assertEquals(0, transaction.getAmount().compareTo(new BigDecimal("25.00")));

        assertEquals(txBefore + 1, transactionRepository.count(),
                "Expected exactly 1 new transaction");
        assertEquals(outboxBefore + 1, outboxEventRepository.count(),
                "Expected exactly 1 new outbox event");
    }
}
