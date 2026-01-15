package banking.core.service;

import banking.core.dto.requests.TransferRequest;
import banking.core.dto.responses.TransferResponse;
import banking.core.error.exception.BankAccountNotFoundException;
import banking.core.error.exception.TransferBusinessException;
import banking.core.model.entity.BankAccount;
import banking.core.model.entity.Transaction;
import banking.core.model.enums.AccountStatus;
import banking.core.model.enums.TransactionStatus;
import banking.core.model.enums.TransactionType;
import banking.core.repository.BankAccountRepository;
import banking.core.repository.TransactionRepository;
import banking.core.service.publisher.TransferOutboxPublisher;
import banking.core.service.validator.TransferValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferValidator transferValidator;
    private final TransferOutboxPublisher transferOutboxPublisher;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransferResponse transfer(UUID userId, TransferRequest request) {
        transferValidator.validatedRequest(request);

        var sourceBankAccountId = request.getFromAccountId();
        var destinationBankAccountId = request.getToAccountId();
        var amount = request.getAmount();

        UUID firstId = sourceBankAccountId;
        UUID secondId = destinationBankAccountId;
        if (firstId.compareTo(secondId) > 0) {
            firstId = destinationBankAccountId;
            secondId = sourceBankAccountId;
        }

        var firstBankAccountOptional = bankAccountRepository.findByIdForUpdate(firstId);
        if (firstBankAccountOptional.isEmpty()) {
            throw new BankAccountNotFoundException(firstId);
        }
        var secondBankAccountOptional = bankAccountRepository.findByIdForUpdate(secondId);
        if (secondBankAccountOptional.isEmpty()) {
            throw new BankAccountNotFoundException(secondId);
        }

        BankAccount firstBankAccount = firstBankAccountOptional.get();
        BankAccount secondBankAccount = secondBankAccountOptional.get();

        BankAccount sourceBankAccount;
        BankAccount destinationBankAccount;

        if (sourceBankAccountId.equals(firstBankAccount.getId())) {
            sourceBankAccount = firstBankAccount;
        } else {
            sourceBankAccount = secondBankAccount;
        }

        if (destinationBankAccountId.equals(firstBankAccount.getId())) {
            destinationBankAccount = firstBankAccount;
        } else {
            destinationBankAccount = secondBankAccount;
        }

        if (!sourceBankAccount.getUserId().equals(userId)) {
            throw new TransferBusinessException("Source Bank Account does not belong to the current user");
        }
        if (sourceBankAccount.getStatus() == AccountStatus.CLOSED) {
            throw new TransferBusinessException("Source Bank Account is closed");
        }
        if (sourceBankAccount.getStatus() == AccountStatus.BLOCKED) {
            throw new TransferBusinessException("Source Bank Account is blocked");
        }
        if (destinationBankAccount.getStatus() == AccountStatus.CLOSED) {
            throw new TransferBusinessException("Destination Bank Account is closed");
        }
        if (destinationBankAccount.getStatus() == AccountStatus.BLOCKED) {
            throw new TransferBusinessException("Destination Bank Account is blocked");
        }
        if (sourceBankAccount.getCurrency() != destinationBankAccount.getCurrency()) {
            throw new TransferBusinessException("Bank Accounts must have same currency");
        }
        if (sourceBankAccount.getBalance().compareTo(amount) < 0) {
            throw new TransferBusinessException("Insufficient funds");
        }

        sourceBankAccount.setBalance(sourceBankAccount.getBalance().subtract(amount));
        destinationBankAccount.setBalance(destinationBankAccount.getBalance().add(amount));

        bankAccountRepository.save(sourceBankAccount);
        bankAccountRepository.save(destinationBankAccount);

        Transaction transaction = Transaction.builder()
                .fromAccount(sourceBankAccount)
                .toAccount(destinationBankAccount)
                .amount(amount)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        transferOutboxPublisher.saveTransferEvent(userId, sourceBankAccount, destinationBankAccount, amount,
                savedTransaction);

        log.info("Transfer completed: transactionId={}, from={}, to={}, amount={}",
                savedTransaction.getId(), sourceBankAccountId, destinationBankAccountId, amount);

        return new TransferResponse(savedTransaction.getId());
    }
}
