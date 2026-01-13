package banking.core.repository;

import banking.core.model.entity.OutboxEvent;
import banking.core.model.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop100ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(EventStatus status, int maxRetries);

    @Modifying
    @Query("DELETE FROM OutboxEvent oe WHERE oe.status = :status AND oe.createdAt < :cutoff")
    int deleteOldEvents(@Param("status") EventStatus status, @Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE OutboxEvent oe SET oe.status = :inProgress WHERE oe.id = :id AND oe.status = :pending")
    int markInProgress(@Param("id") Long id,
                       @Param("pending") EventStatus pending,
                       @Param("inProgress") EventStatus inProgress);

    @Modifying
    @Query("UPDATE OutboxEvent oe SET oe.status = :sent, oe.processedAt = :processedAt" +
            " WHERE oe.id = :id AND oe.status = :inProgress")
    int markSent(@Param("id") Long id,
                 @Param("inProgress") EventStatus inProgress,
                 @Param("sent") EventStatus sent,
                 @Param("processedAt") LocalDateTime processedAt);

    @Modifying
    @Query("UPDATE OutboxEvent oe SET oe.status = :status, oe.retryCount = oe.retryCount + 1, " +
            "oe.errorReason = :error WHERE oe.id = :id AND oe.status = :inProgress")
    void markFailedOrRetry(@Param("id") Long id,
                          @Param("inProgress") EventStatus inProgress,
                          @Param("status") EventStatus status,
                          @Param("error") String error);

    @Modifying
    @Query("UPDATE OutboxEvent oe SET oe.status = :pending WHERE oe.status = :inProgress AND oe.createdAt < :time")
    int rollbackInCaseOfUnexpectedFailure(@Param("pending") EventStatus pending,
                                          @Param("inProgress") EventStatus inProgress,
                                          @Param("time") LocalDateTime time);
}
