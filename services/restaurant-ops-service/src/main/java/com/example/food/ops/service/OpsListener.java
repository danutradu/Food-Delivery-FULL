package com.example.food.ops.service;

import fd.restaurant.RestaurantAcceptanceRequestedV1;
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
public class OpsListener {
    private final OpsService opsService;

    @RetryableTopic(
            attempts = "${kafka.retry.attempts}",
            backoff = @Backoff(delayExpression = "${kafka.retry.delay}", multiplierExpression = "${kafka.retry.multiplier}", maxDelayExpression = "${kafka.retry.max-delay}"),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(id = "ops-acceptance-requests", topics = "fd.restaurant.acceptance-requested.v1", groupId = "restaurant-ops-service")
    public void onAcceptanceRequested(RestaurantAcceptanceRequestedV1 event) {
        opsService.processAcceptanceRequest(event.getOrderId(), event.getRestaurantId());
    }
}
