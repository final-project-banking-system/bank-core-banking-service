package banking.core.service.processor;

import banking.core.model.enums.EventStatus;
import banking.core.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxTxService {
    private final OutboxEventRepository outboxEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryMarkInProgress(Long id) {
        var result = outboxEventRepository.markInProgress(id, EventStatus.PENDING, EventStatus.IN_PROGRESS);
        return result > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryMarkSent(Long id) {
        var result = outboxEventRepository.markSent(id, EventStatus.IN_PROGRESS, EventStatus.SENT, LocalDateTime.now());
        return result > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailure(Long id, int maxRetries, String error) {
        var eventOptional = outboxEventRepository.findById(id);
        if (eventOptional.isPresent()) {
            int currentRetry = eventOptional.get().getRetryCount();
            int nextRetry = currentRetry + 1;

            EventStatus nextStatus;
            if (nextRetry >= maxRetries) {
                nextStatus = EventStatus.FAILED;
            } else {
                nextStatus = EventStatus.PENDING;
            }

            outboxEventRepository.markFailedOrRetry(id, EventStatus.IN_PROGRESS, nextStatus, error);

        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int rollback(LocalDateTime time) {
        return outboxEventRepository.rollbackInCaseOfUnexpectedFailure(EventStatus.PENDING,
                EventStatus.IN_PROGRESS, time);
    }
}
