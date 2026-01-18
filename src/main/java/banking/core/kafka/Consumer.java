package banking.core.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Consumer {
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"${banking.kafka.topics.transfers}",
            "${banking.kafka.topics.accounts}",
            "${banking.kafka.topics.transactions}",
            "${banking.kafka.topics.systemErrors}"},
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(String message, Acknowledgment acknowledgment) {
        try {
            var root = objectMapper.readTree(message);
            var eventType = root.path("eventType").asText();
            log.info("Consumer received eventType={}, data={}", eventType, root.path("data"));
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Consumer failed to process message: {}", message, e);
        }
    }
}
