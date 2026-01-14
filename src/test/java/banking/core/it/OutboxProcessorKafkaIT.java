package banking.core.it;

import banking.core.model.entity.OutboxEvent;
import banking.core.model.enums.EventStatus;
import banking.core.repository.OutboxEventRepository;
import banking.core.service.processor.OutboxProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OutboxProcessorKafkaIT extends IntegrationTestBase{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    OutboxProcessor outboxProcessor;

    @BeforeEach
    void cleanDb() {
        outboxEventRepository.deleteAll();
    }

    @Test
    public void processOutboxMessages_sendsToKafka_andMarksSent() {
        UUID aggregateId = UUID.randomUUID();
        String topic = "banking.transfers";

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("eventType", "TRANSFER_CREATED");
        payload.put("transferId", aggregateId.toString());
        payload.put("amount", "100.00");

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("TRANSFER")
                .aggregateId(aggregateId)
                .topic(topic)
                .payload(payload)
                .status(EventStatus.PENDING)
                .retryCount(0)
                .build();

        event = outboxEventRepository.save(event);

        try (KafkaConsumer<String, String> consumer = createConsumer("it-group-" + UUID.randomUUID())) {
            consumer.subscribe(Collections.singletonList(topic));

            outboxProcessor.processOutboxMessages();

            boolean received = false;
            String receivedKey = null;
            String receivedValue = null;

            Instant deadline = Instant.now().plusSeconds(10);
            while (Instant.now().isBefore(deadline) && !received) {
                var records = consumer.poll(Duration.ofMillis(300));
                if (!records.isEmpty()) {
                    var record = records.iterator().next();
                    received = true;
                    receivedKey = record.key();
                    receivedValue = record.value();
                }
            }

            assertTrue(received, "Expected Kafka message to be received");
            assertEquals(aggregateId.toString(), receivedKey);
            assertNotNull(receivedValue);
            assertTrue(receivedValue.contains("\"eventType\":\"TRANSFER_CREATED\""));

            OutboxEvent updated = waitUntilSent(event.getId(), 10);
            assertEquals(EventStatus.SENT, updated.getStatus());
            assertNotNull(updated.getProcessedAt(), "processedAt should be set for SENT events");
        }
    }

    private KafkaConsumer<String, String> createConsumer(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }

    private OutboxEvent waitUntilSent(Long eventId, int seconds) {
        Instant deadline = Instant.now().plusSeconds(seconds);

        while (Instant.now().isBefore(deadline)) {
            OutboxEvent e = outboxEventRepository.findById(eventId).orElseThrow();
            if (e.getStatus() == EventStatus.SENT) return e;

            try { Thread.sleep(150); }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }

        OutboxEvent last = outboxEventRepository.findById(eventId).orElseThrow();
        fail("Outbox event was not marked SENT in time. Current status=" + last.getStatus()
                + ", retryCount=" + last.getRetryCount()
                + ", errorReason=" + last.getErrorReason());
        return last;
    }
}
