package banking.core.error.handler;

import banking.core.error.exception.BankAccountNotFoundException;
import banking.core.error.exception.TransferBusinessException;
import banking.core.error.exception.ValidationException;
import banking.core.service.publisher.SystemErrorPublisher;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final SystemErrorPublisher systemErrorPublisher;

    private Map<String, Object> buildBody(HttpStatus status, String message) {
        return Map.of("timestamp", LocalDateTime.now().toString(), "status", status.value(),
                "error", status.getReasonPhrase(), "message", message);
    }


    private Map<String, Object> buildBody(HttpStatus status, String message, List<String> errors) {
        return Map.of("timestamp", LocalDateTime.now().toString(), "status", status.value(),
                "error", status.getReasonPhrase(), "message", message, "errors", errors);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException e) {
        return ResponseEntity.badRequest().body(buildBody(HttpStatus.BAD_REQUEST, "Request validation failed",
                e.getErrors()));
    }

    @ExceptionHandler(BankAccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(BankAccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildBody(HttpStatus.NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(TransferBusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(TransferBusinessException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(buildBody(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(buildBody(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBodyValidation(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest().body(buildBody(HttpStatus.BAD_REQUEST,
                "Request validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleParamValidation(ConstraintViolationException e) {
        List<String> errors = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();

        return ResponseEntity.badRequest().body(buildBody(HttpStatus.BAD_REQUEST,
                "Request validation failed", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(buildBody(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception e) {
        systemErrorPublisher.publish(
                "core-banking-service",
                "UNHANDLED",
                "Unhandled exception",
                e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildBody(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error"));
    }
}
