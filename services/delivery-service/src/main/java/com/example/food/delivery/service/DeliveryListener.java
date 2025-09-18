package com.example.food.delivery.service;

import fd.delivery.DeliveryRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryListener {

    private final DeliveryService deliveryService;

    @RetryableTopic(
            attempts = "${kafka.retry.attempts}",
            backoff = @Backoff(delayExpression = "${kafka.retry.delay}", multiplierExpression = "${kafka.retry.multiplier}", maxDelayExpression = "${kafka.retry.max-delay}"),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(id = "delivery-requests", topics = "fd.delivery.requested.v1", groupId = "delivery-service")
    public void onDeliveryRequested(DeliveryRequestedV1 event) {
        deliveryService.processDeliveryRequest(event);
    }
}
