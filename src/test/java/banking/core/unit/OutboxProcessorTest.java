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
        UUID aggregateId = UUID.randomUUID();

        OutboxEvent event = createEvent(1L, "TRANSACTION", aggregateId, "banking.transfers",
                createPayload("TRANSFER_COMPLETED"));

        when(outboxEventRepository.findTop100ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(eq(EventStatus.PENDING),
                anyInt())).thenReturn(List.of(event));

        when(outboxTxService.tryMarkInProgress(1L)).thenReturn(true);
        when(outboxTxService.tryMarkSent(1L)).thenReturn(true);

        when(kafkaTemplate.send(eq("banking.transfers"), eq(aggregateId.toString()), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxProcessor.processOutboxMessages();

        verify(outboxTxService).tryMarkInProgress(1L);
        verify(kafkaTemplate).send(eq("banking.transfers"), eq(aggregateId.toString()), anyString());
        verify(outboxTxService).tryMarkSent(1L);
    }

    @Test
    public void processOutboxMessages_failure_callsHandleFailure() {
        UUID aggregateId = UUID.randomUUID();

        OutboxEvent event = createEvent(2L, "ERROR", aggregateId, "system.errors",
                createPayload("SYSTEM_ERROR"));

        when(outboxEventRepository.findTop100ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(eq(EventStatus.PENDING),
                anyInt())).thenReturn(List.of(event));

        when(outboxTxService.tryMarkInProgress(2L)).thenReturn(true);

        CompletableFuture<SendResult<String, String>> failed = CompletableFuture
                .failedFuture(new RuntimeException("kafka down"));

        when(kafkaTemplate.send(eq("system.errors"), eq(aggregateId.toString()), anyString()))
                .thenReturn(failed);

        outboxProcessor.processOutboxMessages();

        verify(outboxTxService).tryMarkInProgress(2L);
        verify(outboxTxService).handleFailure(eq(2L), eq(3), contains("kafka down"));
    }

    private static OutboxEvent createEvent(Long id, String aggregateType, UUID aggregateId, String topic, ObjectNode payload) {
        return OutboxEvent.builder()
                .id(id)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .topic(topic)
                .payload(payload)
                .status(EventStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static ObjectNode createPayload(String eventType) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("eventType", eventType);
        return node;
    }
}
