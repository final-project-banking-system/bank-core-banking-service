package banking.core.service.publisher;

import banking.core.model.entity.BankAccount;
import banking.core.model.entity.OutboxEvent;
import banking.core.repository.OutboxEventRepository;
import banking.core.service.publisher.util.OutboxJsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountOutboxPublisher {
    @Value("${banking.kafka.topics.accounts}")
    private String topicAccounts;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxJsonUtil outboxJsonUtil;

    public void saveBankAccountEvent(String eventType, UUID userId, BankAccount account) {
        JsonNode payload = createBankAccountEventPayload(eventType, userId, account);

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("ACCOUNT")
                .aggregateId(account.getId())
                .topic(topicAccounts)
                .payload(payload)
                .build());
    }

    private JsonNode createBankAccountEventPayload(String eventType, UUID userId, BankAccount account) {
        Map<String, Object> data = Map.of(
                "eventType", eventType,
                "accountId", account.getId(),
                "userId", userId,
                "accountNumber", account.getAccountNumber(),
                "currency", account.getCurrency().name(),
                "status", account.getStatus().name(),
                "balance", account.getBalance(),
                "occurredAt", java.time.LocalDateTime.now().toString()
        );

        return outboxJsonUtil.toJsonNode(data, "ACCOUNT event, accountId=" + account.getId());
    }
}
