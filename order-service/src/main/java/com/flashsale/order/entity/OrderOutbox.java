package com.flashsale.order.entity;

import com.flashsale.common.outbox.OutboxEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Concrete JPA entity extending OutboxEvent for the order domain, mapping to the order_outbox table in flashsale_order.
 */
@Entity
@Table(name = "order_outbox", indexes = {
        @Index(name = "idx_order_outbox_status_created", columnList = "status, createdAt"),
        @Index(name = "idx_order_outbox_event_id", columnList = "eventId")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OrderOutbox extends OutboxEvent {
}
