package com.example.food.order.messaging;

import fd.payment.PaymentAuthorizedV1;
import fd.restaurant.RestaurantAcceptanceRequestedV1;
import fd.restaurant.RestaurantAcceptedV1;
import fd.delivery.DeliveryRequestedV1;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderSagaListeners {

  private final KafkaTemplate<String, Object> kafka;

  @KafkaListener(id="order-on-payment-auth", topics="fd.payment.authorized.v1", groupId = "order-service")
  public void onPaymentAuthorized(PaymentAuthorizedV1 evt) {
    RestaurantAcceptanceRequestedV1 out = new RestaurantAcceptanceRequestedV1(
        UUID.randomUUID(),
        Instant.now(),
        evt.getOrderId(),
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        evt.getAmountCents(),
        evt.getCurrency()
    );
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.restaurant.acceptance-requested.v1", evt.getOrderId().toString(), out);
    rec.headers().add("eventType", "fd.restaurant.RestaurantAcceptanceRequestedV1".getBytes());
    kafka.send(rec);
  }

  @KafkaListener(id="order-on-restaurant-accepted", topics="fd.restaurant.accepted.v1", groupId = "order-service")
  public void onRestaurantAccepted(RestaurantAcceptedV1 evt) {
    DeliveryRequestedV1 out = new DeliveryRequestedV1(
        UUID.randomUUID(),
        Instant.now(),
        evt.getOrderId(),
        evt.getRestaurantId(),
        UUID.fromString("00000000-0000-0000-0000-000000000000")
    );
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.delivery.requested.v1", evt.getOrderId().toString(), out);
    rec.headers().add("eventType", "fd.delivery.DeliveryRequestedV1".getBytes());
    kafka.send(rec);
  }
}
