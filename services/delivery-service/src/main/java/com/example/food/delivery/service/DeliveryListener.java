package com.example.food.delivery.service;

import fd.delivery.DeliveryRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryListener {

    private final DeliveryService deliveryService;

    @KafkaListener(id = "delivery-requests", topics = "fd.delivery.requested.v1", groupId = "delivery-service")
    public void onDeliveryRequested(DeliveryRequestedV1 event) {
        deliveryService.processDeliveryRequest(event);
    }
}
