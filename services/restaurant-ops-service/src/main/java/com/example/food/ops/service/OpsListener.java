package com.example.food.ops.service;

import com.example.food.ops.config.StandardRetryableTopic;
import fd.order.OrderCancelledV1;
import fd.restaurant.RestaurantAcceptanceRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpsListener {
    private final OpsService opsService;

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.acceptance-requested}", groupId = "${kafka.consumer.group-id}")
    public void onAcceptanceRequested(RestaurantAcceptanceRequestedV1 event) {
        opsService.processAcceptanceRequest(event.getOrderId(), event.getRestaurantId());
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.order-cancelled}", groupId = "${kafka.consumer.group-id}")
    public void onOrderCancelled(OrderCancelledV1 event) {
        opsService.cancelKitchenTicket(event.getOrderId(), event.getReason());
    }
}
