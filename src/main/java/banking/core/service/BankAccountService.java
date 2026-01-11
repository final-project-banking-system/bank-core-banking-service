package banking.core.service;

import banking.core.dto.requests.BalanceOperationRequest;
import banking.core.dto.requests.CreateBankAccountRequest;
import banking.core.dto.requests.UpdateAccountStatusRequest;
import banking.core.dto.responses.BalanceResponse;
import banking.core.dto.responses.BankAccountResponse;
import banking.core.dto.responses.TransferResponse;
import banking.core.error.exception.BankAccountNotFoundException;
import banking.core.error.exception.TransferBusinessException;
import banking.core.mapper.BankAccountMapper;
import banking.core.model.entity.BankAccount;
import banking.core.model.entity.OutboxEvent;
import banking.core.model.entity.Transaction;
import banking.core.model.enums.AccountStatus;
import banking.core.model.enums.TransactionStatus;
import banking.core.model.enums.TransactionType;
import banking.core.repository.BankAccountRepository;
import banking.core.repository.OutboxEventRepository;
import banking.core.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountService {
    private static final String TOPIC_ACCOUNTS = "banking.accounts";
    private static final String TOPIC_TRANSACTIONS = "banking.transactions";

    private final BankAccountRepository bankAccountRepository;
    private final BankAccountMapper bankAccountMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public BankAccountResponse createBankAccount(UUID userId, CreateBankAccountRequest request) {
        BankAccount bankAccount = BankAccount.builder()
                .userId(userId)
                .currency(request.getCurrency())
                .accountNumber(generateAccountNumber())
                .build();

        BankAccount savedAccount = bankAccountRepository.save(bankAccount);

        saveBankAccountEvent("ACCOUNT_CREATED", userId, savedAccount);

        log.info("Bank Account created: accountId={}, userId={}", savedAccount.getId(), userId);

        return bankAccountMapper.toResponse(savedAccount);
    }

    public List<BankAccountResponse> listOfBankAccounts(UUID userId) {
        return bankAccountRepository.findByUserId(userId)
                .stream()
                .map(bankAccountMapper::toResponse)
                .toList();
    }

    public BalanceResponse getBalance(UUID userId, UUID accountId) {
        BankAccount bankAccount = bankAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BankAccountNotFoundException(accountId));
        return new BalanceResponse(bankAccount.getId(), bankAccount.getBalance(), bankAccount.getCurrency());
    }

    public BankAccountResponse getBankAccount(UUID userId, UUID accountId) {
        BankAccount bankAccount = bankAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BankAccountNotFoundException(accountId));
        return bankAccountMapper.toResponse(bankAccount);
    }

    @Transactional
    public BankAccountResponse updateStatus(UUID userId, UUID accountId, UpdateAccountStatusRequest request) {
        BankAccount bankAccount = bankAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BankAccountNotFoundException(accountId));
        bankAccount.setStatus(request.getStatus());

        BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);

        saveBankAccountEvent("ACCOUNT_STATUS_CHANGED", userId, savedBankAccount);

        log.info("Bank Account status changed: accountId={}, userId={}, status={}", savedBankAccount.getId(), userId,
                savedBankAccount.getStatus());

        return bankAccountMapper.toResponse(savedBankAccount);
    }

    @Transactional
    public void closeBankAccount(UUID userId, UUID accountId) {
        BankAccount bankAccount = bankAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BankAccountNotFoundException(accountId));
        bankAccount.setStatus(AccountStatus.CLOSED);

        BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);

        saveBankAccountEvent("ACCOUNT_CLOSED", userId, savedBankAccount);

        log.info("Bank Account closed: accountId={}, userId={}", accountId, userId);
    }

    @Transactional
    public TransferResponse deposit(UUID userId, UUID accountId, BalanceOperationRequest request) {
        BigDecimal amount = request.getAmount();

        BankAccount account = bankAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BankAccountNotFoundException(accountId));

        ensureBankAccountIsActive(account, "Deposit");

        account.setBalance(account.getBalance().add(amount));
        bankAccountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .fromAccount(null)
                .toAccount(account)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        saveTransactionEvent("DEPOSIT_COMPLETED", userId, savedTransaction, null, account, amount);

        log.info("Deposit completed: txId={}, accountId={}, userId={}, amount={}", savedTransaction.getId(), accountId,
                userId, amount);

        return new TransferResponse(savedTransaction.getId());
    }

    @Transactional
    public TransferResponse withdraw(UUID userId, UUID accountId, BalanceOperationRequest request) {
        BigDecimal amount = request.getAmount();

        BankAccount account = bankAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BankAccountNotFoundException(accountId));

        ensureBankAccountIsActive(account, "Withdraw");

        if (account.getBalance().compareTo(amount) < 0) {
            throw new TransferBusinessException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(amount));
        bankAccountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .fromAccount(account)
                .toAccount(null)
                .amount(amount)
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        saveTransactionEvent("WITHDRAWAL_COMPLETED", userId, savedTransaction, account, null, amount);

        log.info("Withdrawal completed: txId={}, accountId={}, userId={}, amount={}", savedTransaction.getId(),
                accountId, userId, amount);

        return new TransferResponse(savedTransaction.getId());
    }

    private String generateAccountNumber() {
        return "ACC-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4);
    }

    private String createBankAccountEventPayload(String eventType, UUID userId, BankAccount account) {
        Map<String, Object> data = Map.of(
                "eventType", eventType,
                "accountId", account.getId(),
                "userId", userId,
                "accountNumber", account.getAccountNumber(),
                "currency", account.getCurrency().name(),
                "status", account.getStatus().name(),
                "balance", account.getBalance(),
                "occurredAt", java.time.LocalDateTime.now().toString()
        );

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload for bankAccountId={}: {}", account.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create outbox payload", e);
        }
    }

    private void saveBankAccountEvent(String eventType, UUID userId, BankAccount account) {
        String payload = createBankAccountEventPayload(eventType, userId, account);

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("ACCOUNT")
                .aggregateId(account.getId())
                .topic(TOPIC_ACCOUNTS)
                .payload(payload)
                .build());
    }

    private void ensureBankAccountIsActive(BankAccount account, String operation) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new TransferBusinessException(operation + " failed: Account is closed");
        }
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new TransferBusinessException(operation + " failed: Account is blocked");
        }
    }

    private void saveTransactionEvent(String eventType, UUID userId, Transaction transaction, BankAccount fromAccount,
                                      BankAccount toAccount, BigDecimal amount) {

        String payload = createTransactionEventPayload(eventType, userId, transaction, fromAccount, toAccount, amount);

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("TRANSACTION")
                .aggregateId(transaction.getId())
                .topic(TOPIC_TRANSACTIONS)
                .payload(payload)
                .build());
    }

    private String createTransactionEventPayload(String eventType, UUID userId, Transaction transaction,
                                                 BankAccount fromAccount, BankAccount toAccount, BigDecimal amount) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventType", eventType);
        data.put("transactionId", transaction.getId());
        data.put("userId", userId);
        data.put("type", transaction.getType().name());
        data.put("status", transaction.getStatus().name());
        data.put("fromAccountId", fromAccount == null ? null : fromAccount.getId());
        data.put("toAccountId", toAccount == null ? null : toAccount.getId());
        data.put("amount", amount);

        String currency;
        if (toAccount != null) {
            currency = toAccount.getCurrency().name();
        } else if (fromAccount != null) {
            currency = fromAccount.getCurrency().name();
        } else {
            currency = null;
        }
        data.put("currency", currency);

        data.put("occurredAt", LocalDateTime.now().toString());

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload for txId={}: {}", transaction.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create outbox payload", e);
        }
    }
}
