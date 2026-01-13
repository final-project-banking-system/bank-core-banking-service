package banking.core.model.entity;

import banking.core.model.enums.EventStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status = EventStatus.PENDING;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "error_reason", length = 1000)
    private String errorReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
