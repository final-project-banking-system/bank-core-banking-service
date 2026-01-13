package banking.core.service.publisher.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxJsonUtil {
    private final ObjectMapper objectMapper;

    public JsonNode toJsonNode(Object payload, String context) {
        try {
            return objectMapper.valueToTree(payload);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to serialize outbox payload: " + context, e);
        }
    }
}
