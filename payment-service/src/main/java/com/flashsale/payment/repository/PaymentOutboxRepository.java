package com.flashsale.payment.repository;

import com.flashsale.common.outbox.OutboxStatus;
import com.flashsale.payment.entity.PaymentOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for polling pending payment outbox events and saving delivery lifecycle mutations.
 */
@Repository
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    Optional<PaymentOutbox> findByEventId(String eventId);

    @Query("SELECT p FROM PaymentOutbox p " +
           "WHERE (p.status = :pendingStatus OR (p.status = :failedStatus AND p.retryCount < :maxRetries)) " +
           "ORDER BY p.createdAt ASC")
    List<PaymentOutbox> findEventsToPublish(
            @Param("pendingStatus") OutboxStatus pendingStatus,
            @Param("failedStatus") OutboxStatus failedStatus,
            @Param("maxRetries") int maxRetries,
            Pageable pageable
    );
}
