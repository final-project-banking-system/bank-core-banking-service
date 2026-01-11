package banking.core.error.exception;

import java.util.UUID;

public class BankAccountNotFoundException extends RuntimeException {
    public BankAccountNotFoundException(UUID accountId) {
        super("Bank account not found: " + accountId);
    }
}
