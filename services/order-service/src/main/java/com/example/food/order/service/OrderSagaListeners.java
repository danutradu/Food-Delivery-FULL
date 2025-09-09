package com.example.food.order.service;

import com.example.food.order.repository.OrderRepository;
import fd.payment.PaymentAuthorizedV1;
import fd.restaurant.RestaurantAcceptanceRequestedV1;
import fd.restaurant.RestaurantAcceptedV1;
import fd.delivery.DeliveryRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaListeners {

  private final KafkaTemplate<String, Object> kafka;
  private final OrderRepository orderRepository;

  @KafkaListener(id="order-on-payment-auth", topics="fd.payment.authorized.v1", groupId = "order-service")
  public void onPaymentAuthorized(PaymentAuthorizedV1 evt) {
    UUID orderId = evt.getOrderId();
    log.info("KAFKA RECV topic=fd.payment.authorized.v1 orderId={}", orderId);

    var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

    RestaurantAcceptanceRequestedV1 out = new RestaurantAcceptanceRequestedV1(
        UUID.randomUUID(),
        Instant.now(),
        evt.getOrderId(),
        order.getRestaurantId(),
        evt.getAmountCents(),
        evt.getCurrency()
    );
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.restaurant.acceptance-requested.v1", evt.getOrderId().toString(), out);
    rec.headers().add("eventType", "fd.restaurant.RestaurantAcceptanceRequestedV1".getBytes());
    kafka.send(rec);
  }

  @KafkaListener(id="order-on-restaurant-accepted", topics="fd.restaurant.accepted.v1", groupId = "order-service")
  public void onRestaurantAccepted(RestaurantAcceptedV1 evt) {
    var orderId = evt.getOrderId();
    log.info("KAFKA RECV topic=fd.restaurant.accepted.v1 orderId={}", orderId);
    var order = orderRepository.findById(orderId).orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

    DeliveryRequestedV1 out = new DeliveryRequestedV1(
        UUID.randomUUID(),
        Instant.now(),
        evt.getOrderId(),
        evt.getRestaurantId(),
        order.getCustomerUserId()
    );
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.delivery.requested.v1", evt.getOrderId().toString(), out);
    rec.headers().add("eventType", "fd.delivery.DeliveryRequestedV1".getBytes());
    kafka.send(rec);
  }
}
