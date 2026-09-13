package com.flashsale.inventory.kafka;

import com.flashsale.common.event.OrderEvent;
import com.flashsale.inventory.dto.InventoryReservationRequest;
import com.flashsale.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final InventoryService inventoryService;

    @KafkaListener(
            topics = "${app.kafka.topics.order-cancelled:order.cancelled}",
            groupId = "${spring.kafka.consumer.group-id:inventory-group}"
    )
    public void handleOrderCancelled(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info(
                "Received OrderCancelled event for orderRef: {}, product: {}, partition: {}, offset: {}",
                event.getOrderReference(),
                event.getProductId(),
                partition,
                offset
        );

        InventoryReservationRequest request =
                InventoryReservationRequest.builder()
                        .productId(event.getProductId())
                        .quantity(event.getQuantity())
                        .orderReference(event.getOrderReference())
                        .build();

        try {
            inventoryService.releaseStock(request);

            log.info(
                    "Successfully released stock for cancelled order: {}",
                    event.getOrderReference()
            );

        } catch (Exception ex) {
            log.error(
                    "Failed to release stock for cancelled order: {}",
                    event.getOrderReference(),
                    ex
            );
            throw ex;
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-expired:order.expired}",
            groupId = "${spring.kafka.consumer.group-id:inventory-group}"
    )
    public void handleOrderExpired(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info(
                "Received OrderExpired event for orderRef: {}, product: {}, partition: {}, offset: {}",
                event.getOrderReference(),
                event.getProductId(),
                partition,
                offset
        );

        InventoryReservationRequest request =
                InventoryReservationRequest.builder()
                        .productId(event.getProductId())
                        .quantity(event.getQuantity())
                        .orderReference(event.getOrderReference())
                        .build();

        try {
            inventoryService.releaseStock(request);

            log.info(
                    "Successfully released stock for expired order: {}",
                    event.getOrderReference()
            );

        } catch (Exception ex) {
            log.error(
                    "Failed to release stock for expired order: {}",
                    event.getOrderReference(),
                    ex
            );
            throw ex;
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-paid:order.paid}",
            groupId = "${spring.kafka.consumer.group-id:inventory-group}"
    )
    public void handleOrderPaid(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info(
                "Received OrderPaid event for orderRef: {}, product: {}, partition: {}, offset: {}",
                event.getOrderReference(),
                event.getProductId(),
                partition,
                offset
        );

        try {
            inventoryService.settleOrderDeduction(
                    event.getProductId(),
                    event.getQuantity(),
                    event.getOrderReference()
            );

            log.info(
                    "Successfully settled stock for paid order: {}",
                    event.getOrderReference()
            );

        } catch (Exception ex) {
            log.error(
                    "Failed to settle stock for paid order: {}",
                    event.getOrderReference(),
                    ex
            );
            throw ex;
        }
    }
}