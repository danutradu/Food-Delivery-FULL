package com.example.food.ops.messaging;

import com.example.food.ops.domain.KitchenTicketEntity;
import com.example.food.ops.repo.KitchenTicketRepository;
import fd.restaurant.RestaurantAcceptanceRequestedV1;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OpsListeners {
  private final KitchenTicketRepository tickets;

  @KafkaListener(id="ops-acceptance-requests", topics="fd.restaurant.acceptance-requested.v1", groupId = "restaurant-ops-service")
  public void onAcceptanceRequested(RestaurantAcceptanceRequestedV1 evt) {
    KitchenTicketEntity t = tickets.findByOrderId(evt.getOrderId()).orElseGet(() -> {
      KitchenTicketEntity nt = new KitchenTicketEntity();
      nt.setId(UUID.randomUUID());
      nt.setOrderId(evt.getOrderId());
      nt.setRestaurantId(evt.getRestaurantId());
      nt.setStatus("PENDING");
      return nt;
    });
    tickets.save(t);
  }
}
