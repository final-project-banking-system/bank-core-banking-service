package banking.core.mapper;

import banking.core.dto.responses.TransactionResponse;
import banking.core.model.entity.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    default TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(transaction.getId(),
                transaction.getFromAccount() == null ? null : transaction.getFromAccount().getId(),
                transaction.getToAccount() == null ? null : transaction.getToAccount().getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCreatedAt());
    }
}
