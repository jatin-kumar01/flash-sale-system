package com.flashsale.order.repository;

import com.flashsale.common.outbox.OutboxStatus;
import com.flashsale.order.entity.OrderOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for polling pending outbox messages in FIFO order and managing outbox lifecycle states.
 */
@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, Long> {

    Optional<OrderOutbox> findByEventId(String eventId);

    @Query("SELECT o FROM OrderOutbox o " +
           "WHERE (o.status = :pendingStatus OR (o.status = :failedStatus AND o.retryCount < :maxRetries)) " +
           "ORDER BY o.createdAt ASC")
    List<OrderOutbox> findEventsToPublish(
            @Param("pendingStatus") OutboxStatus pendingStatus,
            @Param("failedStatus") OutboxStatus failedStatus,
            @Param("maxRetries") int maxRetries,
            Pageable pageable
    );
}
