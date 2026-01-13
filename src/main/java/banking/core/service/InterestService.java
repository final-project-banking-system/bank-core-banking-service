package banking.core.service;

import banking.core.model.entity.BankAccount;
import banking.core.model.entity.Transaction;
import banking.core.model.enums.AccountStatus;
import banking.core.model.enums.TransactionStatus;
import banking.core.model.enums.TransactionType;
import banking.core.repository.BankAccountRepository;
import banking.core.repository.TransactionRepository;
import banking.core.service.publisher.TransactionOutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestService {
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionOutboxPublisher transactionOutboxPublisher;

    @Value("${banking.interest.annual-rate}")
    private BigDecimal annualRate;

    @Transactional
    public int applyDailyInterest() {
        BigDecimal dailyRate = annualRate.divide(BigDecimal.valueOf(365), 12, RoundingMode.HALF_UP);
        int numberOfProcessedBankAccounts = 0;
        UUID lastId = null;

        while (true) {
            List<BankAccount> bankAccounts = bankAccountRepository.findForInterestBatch(AccountStatus.ACTIVE,
                    BigDecimal.ZERO, lastId, PageRequest.of(0, 200));
            if (bankAccounts.isEmpty()) {
                break;
            }

            for (BankAccount bankAccount : bankAccounts) {
                var balance = bankAccount.getBalance();
                var interest = balance.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);

                if (interest.compareTo(BigDecimal.ZERO) > 0) {
                    bankAccount.setBalance(balance.add(interest));
                    bankAccountRepository.save(bankAccount);

                    Transaction transaction = Transaction.builder()
                            .toAccount(bankAccount)
                            .amount(interest)
                            .type(TransactionType.INTEREST)
                            .status(TransactionStatus.COMPLETED)
                            .build();
                    Transaction savedTransaction = transactionRepository.save(transaction);

                    transactionOutboxPublisher.saveTransactionEvent("INTEREST_APPLIED",
                            bankAccount.getUserId(), savedTransaction, null, bankAccount, interest);
                    numberOfProcessedBankAccounts++;
                }
            }
            lastId = bankAccounts.get(bankAccounts.size() - 1).getId();
        }
        log.info("Daily interest applied. annualRate={}, processedAccounts={}, at={}", annualRate,
                numberOfProcessedBankAccounts, LocalDateTime.now());
        return numberOfProcessedBankAccounts;
    }
}
