package banking.core.service.processor;

import banking.core.model.entity.OutboxEvent;
import banking.core.model.enums.EventStatus;
import banking.core.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxProcessor {
    private static final int MAX_RETRIES = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxTxService outboxTxService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedRate = 1000)
    public void processOutboxMessages() {
        var events = outboxEventRepository
                .findTop100ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(EventStatus.PENDING, MAX_RETRIES);

        for (OutboxEvent event : events) {
            if (!outboxTxService.tryMarkInProgress(event.getId())) {
                continue;
            }

            kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                    .orTimeout(10, TimeUnit.SECONDS)
                    .whenComplete((result, error) -> {
                        if (error == null) {
                            boolean marked = outboxTxService.tryMarkSent(event.getId());
                            if (marked) {
                                log.info("Outbox sent: id={}, topic={}", event.getId(), event.getTopic());
                            }
                        } else {
                            outboxTxService.handleFailure(event.getId(), MAX_RETRIES, error.getMessage());
                            log.error("Outbox failed: id={}, topic={}, err={}", event.getId(), event.getTopic(),
                                    error.getMessage());
                        }
                    });
        }
    }

    @Scheduled(fixedRate = 60000)
    public void rollback() {
        int rollbacked = outboxTxService.rollback(LocalDateTime.now().minusMinutes(30));
        if (rollbacked > 0) {
            log.warn("Some events were rolled back due to unexpected error: {}", rollbacked);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void archiveProcessedMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = outboxEventRepository.deleteOldEvents(EventStatus.SENT, cutoff);
        log.info("Deleted old outbox SENT events: {}", deleted);
    }
}
