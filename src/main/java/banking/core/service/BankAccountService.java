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
import banking.core.model.entity.Transaction;
import banking.core.model.enums.AccountStatus;
import banking.core.model.enums.TransactionStatus;
import banking.core.model.enums.TransactionType;
import banking.core.repository.BankAccountRepository;
import banking.core.repository.TransactionRepository;
import banking.core.service.publisher.BankAccountOutboxPublisher;
import banking.core.service.publisher.TransactionOutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountService {
    private final BankAccountRepository bankAccountRepository;
    private final BankAccountMapper bankAccountMapper;
    private final TransactionRepository transactionRepository;
    private final TransactionOutboxPublisher transactionOutboxPublisher;
    private final BankAccountOutboxPublisher bankAccountOutboxPublisher;

    @Transactional
    public BankAccountResponse createBankAccount(UUID userId, CreateBankAccountRequest request) {
        BankAccount bankAccount = BankAccount.builder()
                .userId(userId)
                .currency(request.getCurrency())
                .accountNumber(generateAccountNumber())
                .build();

        BankAccount savedAccount = bankAccountRepository.save(bankAccount);

        bankAccountOutboxPublisher.saveBankAccountEvent("ACCOUNT_CREATED", userId, savedAccount);

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
        BankAccount bankAccount = bankAccountRepository.findLockedByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BankAccountNotFoundException(accountId));
        bankAccount.setStatus(request.getStatus());

        BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);

        bankAccountOutboxPublisher.saveBankAccountEvent("ACCOUNT_STATUS_CHANGED", userId, savedBankAccount);

        log.info("Bank Account status changed: accountId={}, userId={}, status={}", savedBankAccount.getId(), userId,
                savedBankAccount.getStatus());

        return bankAccountMapper.toResponse(savedBankAccount);
    }

    @Transactional
    public void closeBankAccount(UUID userId, UUID accountId) {
        BankAccount bankAccount = bankAccountRepository.findLockedByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BankAccountNotFoundException(accountId));
        bankAccount.setStatus(AccountStatus.CLOSED);

        BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);

        bankAccountOutboxPublisher.saveBankAccountEvent("ACCOUNT_CLOSED", userId, savedBankAccount);

        log.info("Bank Account closed: accountId={}, userId={}", accountId, userId);
    }

    @Transactional
    public TransferResponse deposit(UUID userId, UUID accountId, BalanceOperationRequest request) {
        BigDecimal amount = request.getAmount();

        BankAccount account = bankAccountRepository.findLockedByIdAndUserId(accountId, userId)
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

        transactionOutboxPublisher.saveTransactionEvent("DEPOSIT_COMPLETED", userId, savedTransaction,
                null, account, amount);

        log.info("Deposit completed: txId={}, accountId={}, userId={}, amount={}", savedTransaction.getId(), accountId,
                userId, amount);

        return new TransferResponse(savedTransaction.getId());
    }

    @Transactional
    public TransferResponse withdraw(UUID userId, UUID accountId, BalanceOperationRequest request) {
        BigDecimal amount = request.getAmount();

        BankAccount account = bankAccountRepository.findLockedByIdAndUserId(accountId, userId)
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

        transactionOutboxPublisher.saveTransactionEvent("WITHDRAWAL_COMPLETED", userId, savedTransaction,
                account, null, amount);

        log.info("Withdrawal completed: txId={}, accountId={}, userId={}, amount={}", savedTransaction.getId(),
                accountId, userId, amount);

        return new TransferResponse(savedTransaction.getId());
    }

    private String generateAccountNumber() {
        return "ACC-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4);
    }

    private void ensureBankAccountIsActive(BankAccount account, String operation) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new TransferBusinessException(operation + " failed: Account is closed");
        }
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new TransferBusinessException(operation + " failed: Account is blocked");
        }
    }
}
