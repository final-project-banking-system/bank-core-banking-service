package banking.core.unit;

import banking.core.model.entity.OutboxEvent;
import banking.core.model.enums.EventStatus;
import banking.core.repository.OutboxEventRepository;
import banking.core.service.processor.OutboxProcessor;
import banking.core.service.processor.OutboxTxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxProcessorTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxTxService outboxTxService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    @Test
    public void processOutboxMessages_success_marksSent() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        OutboxEvent event = createEvent(eventId, "TRANSACTION", aggregateId, "banking.transfers",
                createPayload("TRANSFER_COMPLETED"));

        when(outboxEventRepository.findTop100ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(eq(EventStatus.PENDING),
                anyInt())).thenReturn(List.of(event));

        when(outboxTxService.tryMarkInProgress(eventId)).thenReturn(true);
        when(outboxTxService.tryMarkSent(eventId)).thenReturn(true);

        when(kafkaTemplate.send(eq("banking.transfers"), eq(aggregateId.toString()), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxProcessor.processOutboxMessages();

        verify(outboxTxService).tryMarkInProgress(eventId);
        verify(kafkaTemplate).send(eq("banking.transfers"), eq(aggregateId.toString()), anyString());
        verify(outboxTxService).tryMarkSent(eventId);
    }

    @Test
    public void processOutboxMessages_failure_callsHandleFailure() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        OutboxEvent event = createEvent(eventId, "ERROR", aggregateId, "system.errors",
                createPayload("SYSTEM_ERROR"));

        when(outboxEventRepository.findTop100ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(eq(EventStatus.PENDING),
                anyInt())).thenReturn(List.of(event));

        when(outboxTxService.tryMarkInProgress(eventId)).thenReturn(true);

        CompletableFuture<SendResult<String, String>> failed = CompletableFuture
                .failedFuture(new RuntimeException("kafka down"));

        when(kafkaTemplate.send(eq("system.errors"), eq(aggregateId.toString()), anyString()))
                .thenReturn(failed);

        outboxProcessor.processOutboxMessages();

        verify(outboxTxService).tryMarkInProgress(eventId);
        verify(outboxTxService).handleFailure(eq(eventId), eq(3), contains("kafka down"));
    }

    private static OutboxEvent createEvent(UUID id, String aggregateType, UUID aggregateId, String topic, ObjectNode payload) {
        var event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .topic(topic)
                .payload(payload)
                .status(EventStatus.PENDING)
                .retryCount(0)
                .build();
        event.setId(id);
        event.setCreatedAt(LocalDateTime.now());

        return event;
    }

    private static ObjectNode createPayload(String eventType) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("eventType", eventType);
        return node;
    }
}
