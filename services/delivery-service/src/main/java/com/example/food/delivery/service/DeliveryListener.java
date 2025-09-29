package com.example.food.delivery.service;

import com.example.food.delivery.config.StandardRetryableTopic;
import fd.delivery.DeliveryRequestedV1;
import fd.order.OrderCancelledV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryListener {

    private final DeliveryService deliveryService;

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.delivery-requested}", groupId = "${kafka.consumer.group-id}")
    public void onDeliveryRequested(DeliveryRequestedV1 event) {
        deliveryService.processDeliveryRequest(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.order-cancelled}", groupId = "${kafka.consumer.group-id}")
    public void onOrderCancelled(OrderCancelledV1 event) {
        deliveryService.cancelDelivery(event.getOrderId(), event.getReason());
    }
}
