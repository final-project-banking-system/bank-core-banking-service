package banking.core.error.exception;

public class TransferBusinessException extends RuntimeException {
    public TransferBusinessException(String message) {
        super(message);
    }
}
