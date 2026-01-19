package banking.core.service.publisher;

import banking.core.model.entity.OutboxEvent;
import banking.core.repository.OutboxEventRepository;
import banking.core.service.publisher.util.OutboxJsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemErrorPublisher {
    @Value("${banking.kafka.topics.systemErrors}")
    private String topicSystemErrors;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxJsonUtil outboxJsonUtil;

    public void publish(String service, String operation, String message, @Nullable Throwable e) {
        var errorId = UUID.randomUUID();

        Map<String, Object> data = Map.of(
                "errorId", errorId,
                "service", service,
                "operation", operation,
                "message", message,
                "exceptionClass", e == null ? null : e.getClass().getName(),
                "exceptionMessage", e == null ? null : e.getMessage(),
                "occurredAt", LocalDateTime.now().toString()
        );

        JsonNode json = outboxJsonUtil.toJsonNode(data, "SYSTEM_ERROR");

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("ERROR")
                .aggregateId(errorId)
                .topic(topicSystemErrors)
                .payload(json)
                .build());

        log.info("System error saved to outbox: errorId={}, operation={}, message={}", errorId, operation, message);
    }
}
