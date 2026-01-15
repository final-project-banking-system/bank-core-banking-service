package banking.core.service.publisher.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxJsonUtil {
    private final ObjectMapper objectMapper;

    public JsonNode toJsonNode(Object payload, String eventType) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("eventType", eventType);
            root.set("data", objectMapper.valueToTree(payload));
            return root;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to serialize outbox payload: " + eventType, e);
        }
    }
}
