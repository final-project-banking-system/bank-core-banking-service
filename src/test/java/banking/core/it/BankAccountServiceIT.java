package banking.core.it;

import banking.core.dto.requests.BalanceOperationRequest;
import banking.core.dto.requests.CreateBankAccountRequest;
import banking.core.dto.requests.UpdateAccountStatusRequest;
import banking.core.model.enums.AccountStatus;
import banking.core.model.enums.Currency;
import banking.core.repository.BankAccountRepository;
import banking.core.repository.OutboxEventRepository;
import banking.core.repository.TransactionRepository;
import banking.core.service.BankAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class BankAccountServiceIT extends IntegrationTestBase {
    @Autowired
    BankAccountService bankAccountService;

    @Autowired
    BankAccountRepository bankAccountRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void cleanDb() {
        transactionRepository.deleteAll();
        outboxEventRepository.deleteAll();
        bankAccountRepository.deleteAll();
    }

    @Test
    public void createAccount_persistsAccount_andCreatesOutboxEvent() {
        var userId = UUID.randomUUID();
        long outboxBefore = outboxEventRepository.count();

        var response = bankAccountService.createBankAccount(userId, new CreateBankAccountRequest(Currency.EUR));
        var savedBankAccount = bankAccountRepository.findById(response.getId()).orElseThrow();

        assertEquals(userId, savedBankAccount.getUserId());
        assertEquals(Currency.EUR, savedBankAccount.getCurrency());

        long outboxAfter = outboxEventRepository.count();
        assertEquals(outboxBefore + 1, outboxAfter, "Expected exactly 1 new outbox event");
    }

    @Test
    public void updateStatus_changesStatus_andWritesOutbox() {
        var userId = UUID.randomUUID();
        var created = bankAccountService.createBankAccount(userId, new CreateBankAccountRequest(Currency.USD));
        long outboxBefore = outboxEventRepository.count();

        var updated = bankAccountService.updateStatus(
                userId,
                created.getId(),
                new UpdateAccountStatusRequest(AccountStatus.BLOCKED)
        );

        assertEquals(AccountStatus.BLOCKED, updated.getStatus());

        long outboxAfter = outboxEventRepository.count();
        assertEquals(outboxBefore + 1, outboxAfter, "Expected exactly 1 new outbox event");
    }

    @Test
    public void deposit_increasesBalance() {
        var userId = UUID.randomUUID();
        var created = bankAccountService.createBankAccount(userId, new CreateBankAccountRequest(Currency.RUB));
        long outboxBefore = outboxEventRepository.count();

        bankAccountService.deposit(userId, created.getId(), new BalanceOperationRequest(new BigDecimal("100.00")));

        var after = bankAccountRepository.findById(created.getId()).orElseThrow();
        assertEquals(0, after.getBalance().compareTo(new BigDecimal("100.00")));

        long outboxAfter = outboxEventRepository.count();
        assertEquals(outboxBefore + 1, outboxAfter, "Expected exactly 1 new outbox event");
    }
}
