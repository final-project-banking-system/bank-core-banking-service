package banking.core.unit;

import banking.core.dto.requests.TransferRequest;
import banking.core.error.exception.ValidationException;
import banking.core.service.validator.TransferValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TransferValidatorTest {
    private static TransferValidator transferValidator;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        transferValidator = new TransferValidator(validator);
    }

    @Test
    public void validatedRequest_ok() {
        var request = new TransferRequest(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"));
        assertDoesNotThrow(() -> transferValidator.validatedRequest(request));
    }

    @Test
    public void validatedRequest_nullRequest_throws() {
        var exception = assertThrows(ValidationException.class, () -> transferValidator.validatedRequest(null));
        assertTrue(exception.getErrors().stream().anyMatch(s -> s.toLowerCase().contains("must not be null")));
    }

    @Test
    public void validatedRequest_sameAccounts_throws() {
        var id = UUID.randomUUID();
        var request = new TransferRequest(id, id, new BigDecimal("10.00"));
        var exception = assertThrows(ValidationException.class, () -> transferValidator.validatedRequest(request));

        assertTrue(exception.getErrors().stream().anyMatch(s -> s.toLowerCase().contains("must be different")));
    }

    @Test
    public void validatedRequest_nonPositiveAmount_throws() {
        var request = new TransferRequest(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO);
        var exception = assertThrows(ValidationException.class, () -> transferValidator.validatedRequest(request));

        assertTrue(exception.getErrors().stream().anyMatch(s -> s.toLowerCase().contains("positive")));
    }
}
