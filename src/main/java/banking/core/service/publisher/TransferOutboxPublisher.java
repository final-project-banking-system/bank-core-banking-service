package banking.core.service.publisher;

import banking.core.model.entity.BankAccount;
import banking.core.model.entity.OutboxEvent;
import banking.core.model.entity.Transaction;
import banking.core.repository.OutboxEventRepository;
import banking.core.service.publisher.util.OutboxJsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferOutboxPublisher {
    @Value("${banking.kafka.topics.transfers}")
    private String topicTransfers;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxJsonUtil outboxJsonUtil;

    public void saveTransferEvent(UUID userId,
                                  BankAccount fromAccount,
                                  BankAccount toAccount,
                                  BigDecimal amount,
                                  Transaction savedTransaction) {

        JsonNode payload = createTransferEventPayload(userId, fromAccount, toAccount, amount, savedTransaction);

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("TRANSACTION")
                .aggregateId(savedTransaction.getId())
                .topic(topicTransfers)
                .payload(payload)
                .build());
    }

    private JsonNode createTransferEventPayload(UUID userId,
                                                BankAccount fromAccount,
                                                BankAccount toAccount,
                                                BigDecimal amount,
                                                Transaction savedTransaction) {
        Map<String, Object> data = Map.of("eventType", "TRANSFER_COMPLETED",
                "transactionId", savedTransaction.getId(),
                "userId", userId,
                "fromAccountId", fromAccount.getId(),
                "toAccountId", toAccount.getId(),
                "amount", amount,
                "currency", fromAccount.getCurrency().name(),
                "occurredAt", java.time.LocalDateTime.now().toString()
        );

        return outboxJsonUtil.toJsonNode(data, "TRANSFER event, txId=" + savedTransaction.getId());
    }
}
