//package com.flashsale.order.publisher;
//
//import com.flashsale.common.outbox.OutboxStatus;
//import com.flashsale.order.entity.OrderOutbox;
//import com.flashsale.order.repository.OrderOutboxRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
///**
// * Scheduled worker polling pending outbox records from order_outbox, publishing them to Kafka,
// * and recording status transitions (PUBLISHED or FAILED).
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class OrderOutboxPublisher {
//
//    private final OrderOutboxRepository outboxRepository;
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    @Value("${app.outbox.max-retries:3}")
//    private int maxRetries;
//
//    @Value("${app.outbox.batch-size:50}")
//    private int batchSize;
//
//    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
//    @Transactional
//    public void publishPendingEvents() {
//        List<OrderOutbox> events = outboxRepository.findEventsToPublish(
//                OutboxStatus.PENDING,
//                OutboxStatus.FAILED,
//                maxRetries,
//                PageRequest.of(0, batchSize)
//        );
//
//        if (events.isEmpty()) {
//            return;
//        }
//
//        log.debug("Found {} order outbox events to publish", events.size());
//
//        for (OrderOutbox event : events) {
//            try {
//                // Send synchronously within worker thread to ensure ACK before marking PUBLISHED
//                kafkaTemplate.send(
//                        event.getDestinationTopic(),
//                        event.getPartitionKey(),
//                        event.getPayload()
//                ).get(3, TimeUnit.SECONDS);
//
//                event.markPublished();
//                log.info("Published outbox event [id={}, type={}, aggregateId={}] to topic: {}",
//                        event.getEventId(), event.getEventType(), event.getAggregateId(), event.getDestinationTopic());
//            } catch (Exception ex) {
//                log.error("Failed to publish outbox event [id={}, aggregateId={}]: {}",
//                        event.getEventId(), event.getAggregateId(), ex.getMessage());
//                event.markFailed(ex.getMessage());
//            }
//            outboxRepository.save(event);
//        }
//    }
//}


package com.flashsale.order.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.event.OrderEvent;
import com.flashsale.common.outbox.OutboxStatus;
import com.flashsale.order.entity.OrderOutbox;
import com.flashsale.order.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxPublisher {

    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.max-retries:3}")
    private int maxRetries;

    @Value("${app.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPendingEvents() {

        List<OrderOutbox> events = outboxRepository.findEventsToPublish(
                OutboxStatus.PENDING,
                OutboxStatus.FAILED,
                maxRetries,
                PageRequest.of(0, batchSize)
        );

        if (events.isEmpty()) {
            return;
        }

        log.debug("Found {} order outbox events to publish", events.size());

        for (OrderOutbox event : events) {
            try {
                OrderEvent orderEvent =
                        objectMapper.readValue(event.getPayload(), OrderEvent.class);

                kafkaTemplate.send(
                        event.getDestinationTopic(),
                        event.getPartitionKey(),
                        orderEvent
                ).get(3, TimeUnit.SECONDS);

                event.markPublished();

                log.info(
                        "Published outbox event [id={}, type={}, aggregateId={}] to topic: {}",
                        event.getEventId(),
                        event.getEventType(),
                        event.getAggregateId(),
                        event.getDestinationTopic()
                );

            } catch (Exception ex) {
                log.error(
                        "Failed to publish outbox event [id={}, aggregateId={}]: {}",
                        event.getEventId(),
                        event.getAggregateId(),
                        ex.getMessage(),
                        ex
                );

                event.markFailed(ex.getMessage());
            }

            outboxRepository.save(event);
        }
    }
}