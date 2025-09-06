package com.example.food.notify.messaging;

import fd.delivery.CourierAssignedV1;
import fd.delivery.OrderDeliveredV1;
import fd.delivery.OrderPickedUpV1;
import fd.order.OrderCreatedV1;
import fd.payment.PaymentAuthorizedV1;
import fd.restaurant.RestaurantAcceptedV1;
import fd.restaurant.RestaurantRejectedV1;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotifyListeners {

  @KafkaListener(id="notify-order-created", topics="fd.order.created.v1", groupId="notification-service")
  public void onOrderCreated(OrderCreatedV1 e) { log.info("Notify: Order created {}", e.getOrderId()); }

  @KafkaListener(id="notify-payment-auth", topics="fd.payment.authorized.v1", groupId="notification-service")
  public void onPaymentAuth(PaymentAuthorizedV1 e) { log.info("Notify: Payment authorized {}", e.getOrderId()); }

  @KafkaListener(id="notify-restaurant-accepted", topics="fd.restaurant.accepted.v1", groupId="notification-service")
  public void onRestaurantAccepted(RestaurantAcceptedV1 e) { log.info("Notify: Restaurant accepted {}", e.getOrderId()); }

  @KafkaListener(id="notify-restaurant-rejected", topics="fd.restaurant.rejected.v1", groupId="notification-service")
  public void onRestaurantRejected(RestaurantRejectedV1 e) { log.info("Notify: Restaurant rejected {}", e.getOrderId()); }

  @KafkaListener(id="notify-courier-assigned", topics="fd.delivery.courier-assigned.v1", groupId="notification-service")
  public void onCourierAssigned(CourierAssignedV1 e) { log.info("Notify: Courier assigned {}", e.getOrderId()); }

  @KafkaListener(id="notify-picked-up", topics="fd.delivery.picked-up.v1", groupId="notification-service")
  public void onPickedUp(OrderPickedUpV1 e) { log.info("Notify: Order picked up {}", e.getOrderId()); }

  @KafkaListener(id="notify-delivered", topics="fd.delivery.delivered.v1", groupId="notification-service")
  public void onDelivered(OrderDeliveredV1 e) { log.info("Notify: Order delivered {}", e.getOrderId()); }
}
