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
        long id = 10L;

        OutboxEvent event = OutboxEvent.builder()
                .id(id)
                .retryCount(2)
                .status(EventStatus.IN_PROGRESS)
                .build();

        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(event));

        outboxTxService.handleFailure(id, 3, "error");

        verify(outboxEventRepository).markFailedOrRetry(eq(id), eq(EventStatus.IN_PROGRESS), eq(EventStatus.FAILED),
                eq("error"));
    }

    @Test
    public void handleFailure_whenStillCanRetry_marksPending() {
        long id = 11L;

        OutboxEvent event = OutboxEvent.builder()
                .id(id)
                .retryCount(0)
                .status(EventStatus.IN_PROGRESS)
                .build();

        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(event));

        outboxTxService.handleFailure(id, 3, "error");

        verify(outboxEventRepository).markFailedOrRetry(eq(id), eq(EventStatus.IN_PROGRESS), eq(EventStatus.PENDING),
                eq("error"));
    }

    @Test
    public void tryMarkInProgress_returnsTrue_whenUpdated() {
        when(outboxEventRepository.markInProgress(1L, EventStatus.PENDING, EventStatus.IN_PROGRESS))
                .thenReturn(1);

        boolean ok = outboxTxService.tryMarkInProgress(1L);

        assertTrue(ok);
        verify(outboxEventRepository).markInProgress(1L, EventStatus.PENDING, EventStatus.IN_PROGRESS);
    }

    @Test
    public void handleFailure_whenEventNotFound_doesNothing() {
        long id = 12L;

        when(outboxEventRepository.findById(id)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> outboxTxService.handleFailure(id, 3, "error"));

        verify(outboxEventRepository, never()).markFailedOrRetry(anyLong(), any(), any(), anyString());
    }

    @Test
    public void tryMarkInProgress_returnsFalse_whenNotUpdated() {
        when(outboxEventRepository.markInProgress(1L, EventStatus.PENDING, EventStatus.IN_PROGRESS))
                .thenReturn(0);

        boolean ok = outboxTxService.tryMarkInProgress(1L);

        assertFalse(ok);
        verify(outboxEventRepository).markInProgress(1L, EventStatus.PENDING, EventStatus.IN_PROGRESS);
    }

    @Test
    public void tryMarkSent_returnsTrue_whenUpdated() {
        when(outboxEventRepository.markSent(eq(5L), eq(EventStatus.IN_PROGRESS), eq(EventStatus.SENT),
                any(LocalDateTime.class))).thenReturn(1);

        boolean ok = outboxTxService.tryMarkSent(5L);

        assertTrue(ok);
        verify(outboxEventRepository).markSent(eq(5L), eq(EventStatus.IN_PROGRESS), eq(EventStatus.SENT),
                any(LocalDateTime.class));
    }

    @Test
    public void tryMarkSent_returnsFalse_whenNotUpdated() {
        when(outboxEventRepository.markSent(eq(5L), eq(EventStatus.IN_PROGRESS), eq(EventStatus.SENT),
                any(LocalDateTime.class)))
                .thenReturn(0);

        boolean ok = outboxTxService.tryMarkSent(5L);

        assertFalse(ok);
        verify(outboxEventRepository).markSent(eq(5L), eq(EventStatus.IN_PROGRESS), eq(EventStatus.SENT),
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
