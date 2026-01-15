package banking.core.service;

import banking.core.dto.responses.TransactionResponse;
import banking.core.error.exception.TransferBusinessException;
import banking.core.mapper.TransactionMapper;
import banking.core.repository.BankAccountRepository;
import banking.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final BankAccountRepository bankAccountRepository;

    public Page<TransactionResponse> getHistoryOfTransactions(UUID userId, UUID accountId, Pageable pageable) {
        bankAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new TransferBusinessException("Account does not belong to current user"));

        return transactionRepository.findByFromAccount_IdOrToAccount_Id(accountId, accountId, pageable)
                .map(transactionMapper::toResponse);
    }
}
