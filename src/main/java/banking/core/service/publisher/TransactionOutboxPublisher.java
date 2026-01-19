package banking.core.service.publisher;

import banking.core.model.entity.BankAccount;
import banking.core.model.entity.OutboxEvent;
import banking.core.model.entity.Transaction;
import banking.core.repository.OutboxEventRepository;
import banking.core.service.publisher.util.OutboxJsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionOutboxPublisher {
    @Value("${banking.kafka.topics.transactions}")
    private String topicTransactions;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxJsonUtil outboxJsonUtil;

    public void saveTransactionEvent(String eventType, UUID userId, Transaction transaction, BankAccount fromAccount,
                                     BankAccount toAccount, BigDecimal amount) {

        JsonNode payload = createTransactionEventPayload(eventType, userId, transaction, fromAccount, toAccount, amount);

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("TRANSACTION")
                .aggregateId(transaction.getId())
                .topic(topicTransactions)
                .payload(payload)
                .build());
    }

    private JsonNode createTransactionEventPayload(String eventType, UUID userId, Transaction transaction,
                                                   BankAccount fromAccount, BankAccount toAccount, BigDecimal amount) {
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", transaction.getId());
        data.put("userId", userId);
        data.put("type", transaction.getType().name());
        data.put("status", transaction.getStatus().name());
        data.put("fromAccountId", fromAccount == null ? null : fromAccount.getId());
        data.put("toAccountId", toAccount == null ? null : toAccount.getId());
        data.put("amount", amount);

        String currency;
        if (toAccount != null) {
            currency = toAccount.getCurrency().name();
        } else if (fromAccount != null) {
            currency = fromAccount.getCurrency().name();
        } else {
            currency = null;
        }
        data.put("currency", currency);

        data.put("occurredAt", LocalDateTime.now().toString());

        return outboxJsonUtil.toJsonNode(data, eventType);
    }
}
