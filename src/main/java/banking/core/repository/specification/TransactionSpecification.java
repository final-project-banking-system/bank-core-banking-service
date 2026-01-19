package banking.core.repository.specification;

import banking.core.model.entity.Transaction;
import banking.core.model.enums.TransactionStatus;
import banking.core.model.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionSpecification {
    public static Specification<Transaction> byAccount(UUID accountId) {
        return (root, query, cb) -> cb.or(cb.equal(
                root.get("fromAccount").get("id"), accountId), cb.equal(root.get("toAccount").get("id"), accountId));
    }

    public static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, cb) -> type == null ? null :
                cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> hasStatus(TransactionStatus status) {
        return (root, query, cb) -> status == null ? null :
                cb.equal(root.get("status"), status);
    }

    public static Specification<Transaction> amountMin(BigDecimal min) {
        return (root, query, cb) -> min == null ? null :
                cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    public static Specification<Transaction> amountMax(BigDecimal max) {
        return (root, query, cb) -> max == null ? null :
                cb.lessThanOrEqualTo(root.get("amount"), max);
    }

    public static Specification<Transaction> createdFrom(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null :
                cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Transaction> createdTo(LocalDateTime to) {
        return (root, query, cb) -> to == null ? null :
                cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
