package banking.core.service.validator;

import banking.core.dto.requests.TransferRequest;
import banking.core.error.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferValidator {
    private final Validator validator;

    public void validatedRequest(TransferRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Transfer Request must not be null");
            throw new ValidationException(errors);
        }

        var violations = validator.validate(request);
        var violationMessages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .toList();
        errors.addAll(violationMessages);

        var sourceBankAccountId = request.getFromAccountId();
        var destinationBankAccountId = request.getToAccountId();
        BigDecimal amount = request.getAmount();

        if (sourceBankAccountId != null && sourceBankAccountId.equals(destinationBankAccountId)) {
            errors.add("Source Bank Account ID and Destination Bank Account ID must be different");
        }

        if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Amount must be positive");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
