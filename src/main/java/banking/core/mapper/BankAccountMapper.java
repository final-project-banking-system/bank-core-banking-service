package banking.core.mapper;

import banking.core.dto.responses.BankAccountResponse;
import banking.core.model.entity.BankAccount;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BankAccountMapper {
    BankAccountResponse toResponse(BankAccount bankAccount);
}
