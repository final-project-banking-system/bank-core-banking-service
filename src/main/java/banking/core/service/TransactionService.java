package banking.core.service;

import banking.core.dto.requests.TransactionFilter;
import banking.core.dto.responses.TransactionResponse;
import banking.core.error.exception.TransferBusinessException;
import banking.core.mapper.TransactionMapper;
import banking.core.model.entity.Transaction;
import banking.core.repository.BankAccountRepository;
import banking.core.repository.TransactionRepository;
import banking.core.repository.specification.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final BankAccountRepository bankAccountRepository;

    public Page<TransactionResponse> getHistoryOfTransactions(TransactionFilter filter, UUID userId, Pageable pageable) {
        UUID accountId = filter.getAccountId();
        if (accountId == null) {
            throw new TransferBusinessException("accountId is required");
        }

        bankAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new TransferBusinessException("Account does not belong to current user"));

        Specification<Transaction> specification = TransactionSpecification.byAccount(filter.getAccountId());

        specification = specification.and(TransactionSpecification.hasType(filter.getType()));
        specification = specification.and(TransactionSpecification.hasStatus(filter.getStatus()));
        specification = specification.and(TransactionSpecification.amountMin(filter.getMinAmount()));
        specification = specification.and(TransactionSpecification.amountMax(filter.getMaxAmount()));
        specification = specification.and(TransactionSpecification.createdFrom(filter.getFromTime()));
        specification = specification.and(TransactionSpecification.createdTo(filter.getToTime()));

        return transactionRepository.findAll(specification, pageable).map(transactionMapper::toResponse);
    }
}
