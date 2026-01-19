package banking.core.repository;

import banking.core.model.entity.BankAccount;
import banking.core.model.enums.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {
    List<BankAccount> findByUserId(UUID userId);

    Optional<BankAccount> findByIdAndUserId(UUID accountId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BankAccount> findLockedByIdAndUserId(UUID accountId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ba FROM BankAccount ba WHERE ba.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.status = :status AND ba.balance > :minBalance " +
            "AND (:lastId IS NULL OR ba.id > :lastId) ORDER BY ba.id ASC")
    List<BankAccount> findForInterestBatch(@Param("status") AccountStatus status,
                                           @Param("minBalance") BigDecimal minBalance,
                                           @Param("lastId") UUID lastId,
                                           Pageable pageable);
}
