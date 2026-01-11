package banking.core.service;

import banking.core.model.entity.OutboxEvent;
import banking.core.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemErrorPublisher {
    private static final String TOPIC_SYSTEM_ERRORS = "system.errors";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void publish(String service, String operation, String message, @Nullable Throwable e) {
        var errorId = UUID.randomUUID();

        Map<String, Object> payload = Map.of(
                "eventType", "SYSTEM_ERROR",
                "errorId", errorId,
                "service", service,
                "operation", operation,
                "message", message,
                "exceptionClass", e == null ? null : e.getClass().getName(),
                "exceptionMessage", e == null ? null : e.getMessage(),
                "occurredAt", LocalDateTime.now().toString()
        );

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            json = "{\"eventType\":\"SYSTEM_ERROR\",\"errorId\":\"" + errorId +
                    "\",\"message\":\"Failed to serialize error payload\"}";
        }

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("ERROR")
                .aggregateId(errorId)
                .topic(TOPIC_SYSTEM_ERRORS)
                .payload(json)
                .build());

        log.warn("System error saved to outbox: errorId={}, operation={}, message={}", errorId, operation, message);
    }
}
