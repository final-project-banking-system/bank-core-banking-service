package banking.core.repository;

import banking.core.model.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByFromAccount_IdOrToAccount_Id(UUID fromId, UUID toId, Pageable pageable);
}
