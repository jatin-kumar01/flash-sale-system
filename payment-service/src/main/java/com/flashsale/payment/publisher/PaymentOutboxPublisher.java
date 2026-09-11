//package com.flashsale.payment.publisher;
//
//import com.flashsale.common.outbox.OutboxStatus;
//import com.flashsale.payment.entity.PaymentOutbox;
//import com.flashsale.payment.repository.PaymentOutboxRepository;
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
// * Scheduled dispatcher polling pending outbox messages from payment_outbox and publishing payment.completed events to Kafka.
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PaymentOutboxPublisher {
//
//    private final PaymentOutboxRepository outboxRepository;
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
//        List<PaymentOutbox> events = outboxRepository.findEventsToPublish(
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
//        log.debug("Found {} payment outbox events to publish", events.size());
//
//        for (PaymentOutbox event : events) {
//            try {
//                kafkaTemplate.send(
//                        event.getDestinationTopic(),
//                        event.getPartitionKey(),
//                        event.getPayload()
//                ).get(3, TimeUnit.SECONDS);
//
//                event.markPublished();
//                log.info("Published payment outbox event [id={}, type={}, orderRef={}] to topic: {}",
//                        event.getEventId(), event.getEventType(), event.getPartitionKey(), event.getDestinationTopic());
//            } catch (Exception ex) {
//                log.error("Failed to publish payment outbox event [id={}, orderRef={}]: {}",
//                        event.getEventId(), event.getPartitionKey(), ex.getMessage());
//                event.markFailed(ex.getMessage());
//            }
//            outboxRepository.save(event);
//        }
//    }
//}

//
//package com.flashsale.payment.publisher;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.flashsale.common.event.PaymentEvent;
//import com.flashsale.common.outbox.OutboxStatus;
//import com.flashsale.payment.entity.PaymentOutbox;
//import com.flashsale.payment.repository.PaymentOutboxRepository;
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
// * Scheduled dispatcher polling pending outbox messages from payment_outbox
// * and publishing payment.completed events to Kafka.
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PaymentOutboxPublisher {
//
//    private final PaymentOutboxRepository outboxRepository;
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//    private final ObjectMapper objectMapper;
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
//
//        List<PaymentOutbox> events = outboxRepository.findEventsToPublish(
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
//        log.debug("Found {} payment outbox events to publish", events.size());
//
//        for (PaymentOutbox event : events) {
//            try {
//
//                // Convert stored JSON payload back into PaymentEvent object
//                PaymentEvent paymentEvent = objectMapper.readValue(
//                        event.getPayload(),
//                        PaymentEvent.class
//                );
//
//                // Send PaymentEvent object to Kafka
//                kafkaTemplate.send(
//                        event.getDestinationTopic(),
//                        event.getPartitionKey(),
//                        paymentEvent
//                ).get(3, TimeUnit.SECONDS);
//
//                event.markPublished();
//
//                log.info(
//                        "Published payment outbox event [id={}, type={}, orderRef={}] to topic: {}",
//                        event.getEventId(),
//                        event.getEventType(),
//                        event.getPartitionKey(),
//                        event.getDestinationTopic()
//                );
//
//            } catch (Exception ex) {
//
//                log.error(
//                        "Failed to publish payment outbox event [id={}, orderRef={}]: {}",
//                        event.getEventId(),
//                        event.getPartitionKey(),
//                        ex.getMessage()
//                );
//
//                event.markFailed(ex.getMessage());
//            }
//
//            outboxRepository.save(event);
//        }
//    }
//}



package com.flashsale.payment.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.event.PaymentEvent;
import com.flashsale.common.outbox.OutboxStatus;
import com.flashsale.payment.entity.PaymentOutbox;
import com.flashsale.payment.repository.PaymentOutboxRepository;
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
public class PaymentOutboxPublisher {

    private final PaymentOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.max-retries:3}")
    private int maxRetries;

    @Value("${app.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPendingEvents() {

        List<PaymentOutbox> events = outboxRepository.findEventsToPublish(
                OutboxStatus.PENDING,
                OutboxStatus.FAILED,
                maxRetries,
                PageRequest.of(0, batchSize)
        );

        if (events.isEmpty()) {
            return;
        }

        log.debug("Found {} payment outbox events to publish", events.size());

        for (PaymentOutbox event : events) {
            try {

                // Convert stored JSON payload into PaymentEvent object
                PaymentEvent paymentEvent = objectMapper.readValue(
                        event.getPayload(),
                        PaymentEvent.class
                );

                // Send PaymentEvent object to Kafka
                kafkaTemplate.send(
                        event.getDestinationTopic(),
                        event.getPartitionKey(),
                        paymentEvent
                ).get(3, TimeUnit.SECONDS);

                // Mark event as successfully published
                event.markPublished();

                log.info(
                        "Published payment outbox event [id={}, type={}, orderRef={}] to topic: {}",
                        event.getEventId(),
                        event.getEventType(),
                        event.getPartitionKey(),
                        event.getDestinationTopic()
                );

            } catch (Exception ex) {

                log.error(
                        "Failed to publish payment outbox event [id={}, orderRef={}]: {}",
                        event.getEventId(),
                        event.getPartitionKey(),
                        ex.getMessage(),
                        ex
                );

                event.markFailed(ex.getMessage());
            }

            outboxRepository.save(event);
        }
    }
}