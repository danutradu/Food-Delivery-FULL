package com.example.food.ops.service;

import com.example.food.ops.model.KitchenTicketEntity;
import com.example.food.ops.repository.KitchenTicketRepository;
import fd.restaurant.RestaurantAcceptanceRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpsListener {
  private final OpsService opsService;

  @KafkaListener(id="ops-acceptance-requests", topics="fd.restaurant.acceptance-requested.v1", groupId = "restaurant-ops-service")
  @Transactional
  public void onAcceptanceRequested(RestaurantAcceptanceRequestedV1 event) {
    log.info("KAFKA RECV topic=fd.restaurant.acceptance-requested.v1 orderId={}", event.getOrderId());
    opsService.processAcceptanceRequest(event.getOrderId(), event.getRestaurantId());
  }
}
