package com.flashsale.common.outbox;

/**
 * Enumeration of lifecycle states for outbox events.
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
