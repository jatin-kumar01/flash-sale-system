package com.flashsale.payment.entity;

import com.flashsale.common.outbox.OutboxEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * JPA entity extending OutboxEvent for the payment domain, mapping to the payment_outbox table in flashsale_payment.
 */
@Entity
@Table(name = "payment_outbox", indexes = {
        @Index(name = "idx_payment_outbox_status_created", columnList = "status, createdAt"),
        @Index(name = "idx_payment_outbox_event_id", columnList = "eventId")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PaymentOutbox extends OutboxEvent {
}
