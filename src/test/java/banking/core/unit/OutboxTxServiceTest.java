package banking.core.unit;

import banking.core.model.entity.OutboxEvent;
import banking.core.model.enums.EventStatus;
import banking.core.repository.OutboxEventRepository;
import banking.core.service.processor.OutboxTxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxTxServiceTest {
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxTxService outboxTxService;

    @Test
    public void handleFailure_whenMaxRetriesReached_marksFailed() {
        UUID id = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.builder()
                .retryCount(2)
                .status(EventStatus.IN_PROGRESS)
                .build();
        event.setId(id);

        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(event));

        outboxTxService.handleFailure(id, 3, "error");

        verify(outboxEventRepository).markFailedOrRetry(eq(id), eq(EventStatus.IN_PROGRESS), eq(EventStatus.FAILED),
                eq("error"));
    }

    @Test
    public void handleFailure_whenStillCanRetry_marksPending() {
        UUID id = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.builder()
                .retryCount(0)
                .status(EventStatus.IN_PROGRESS)
                .build();
        event.setId(id);

        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(event));

        outboxTxService.handleFailure(id, 3, "error");

        verify(outboxEventRepository).markFailedOrRetry(eq(id), eq(EventStatus.IN_PROGRESS), eq(EventStatus.PENDING),
                eq("error"));
    }

    @Test
    public void tryMarkInProgress_returnsTrue_whenUpdated() {
        UUID id = UUID.randomUUID();

        when(outboxEventRepository.markInProgress(id, EventStatus.PENDING, EventStatus.IN_PROGRESS))
                .thenReturn(1);

        boolean ok = outboxTxService.tryMarkInProgress(id);

        assertTrue(ok);
        verify(outboxEventRepository).markInProgress(id, EventStatus.PENDING, EventStatus.IN_PROGRESS);
    }

    @Test
    public void handleFailure_whenEventNotFound_doesNothing() {
        UUID id = UUID.randomUUID();

        when(outboxEventRepository.findById(id)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> outboxTxService.handleFailure(id, 3, "error"));

        verify(outboxEventRepository, never()).markFailedOrRetry(any(UUID.class), any(), any(), anyString());
    }

    @Test
    public void tryMarkInProgress_returnsFalse_whenNotUpdated() {
        UUID id = UUID.randomUUID();

        when(outboxEventRepository.markInProgress(id, EventStatus.PENDING, EventStatus.IN_PROGRESS))
                .thenReturn(0);

        boolean ok = outboxTxService.tryMarkInProgress(id);

        assertFalse(ok);
        verify(outboxEventRepository).markInProgress(id, EventStatus.PENDING, EventStatus.IN_PROGRESS);
    }

    @Test
    public void tryMarkSent_returnsTrue_whenUpdated() {
        UUID id = UUID.randomUUID();

        when(outboxEventRepository.markSent(eq(id), eq(EventStatus.IN_PROGRESS), eq(EventStatus.SENT),
                any(LocalDateTime.class))).thenReturn(1);

        boolean ok = outboxTxService.tryMarkSent(id);

        assertTrue(ok);
        verify(outboxEventRepository).markSent(eq(id), eq(EventStatus.IN_PROGRESS), eq(EventStatus.SENT),
                any(LocalDateTime.class));
    }

    @Test
    public void tryMarkSent_returnsFalse_whenNotUpdated() {
        UUID id = UUID.randomUUID();

        when(outboxEventRepository.markSent(eq(id), eq(EventStatus.IN_PROGRESS), eq(EventStatus.SENT),
                any(LocalDateTime.class)))
                .thenReturn(0);

        boolean ok = outboxTxService.tryMarkSent(id);

        assertFalse(ok);
        verify(outboxEventRepository).markSent(eq(id), eq(EventStatus.IN_PROGRESS), eq(EventStatus.SENT),
                any(LocalDateTime.class));
    }

    @Test
    public void rollback_delegatesToRepository() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        when(outboxEventRepository.rollbackInCaseOfUnexpectedFailure(EventStatus.PENDING, EventStatus.IN_PROGRESS,
                cutoff)).thenReturn(7);

        int result = outboxTxService.rollback(cutoff);

        assertEquals(7, result);
        verify(outboxEventRepository).rollbackInCaseOfUnexpectedFailure(EventStatus.PENDING, EventStatus.IN_PROGRESS,
                cutoff);
    }
}
