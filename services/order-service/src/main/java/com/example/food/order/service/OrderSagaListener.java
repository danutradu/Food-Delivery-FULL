package com.example.food.order.service;

import com.example.food.common.outbox.OutboxService;
import com.example.food.order.config.KafkaTopics;
import com.example.food.order.config.StandardRetryableTopic;
import com.example.food.order.exception.OrderNotFoundException;
import com.example.food.order.model.OrderEntity;
import com.example.food.order.model.OrderStatus;
import com.example.food.order.repository.OrderRepository;
import fd.cart.CartCheckedOutV1;
import fd.delivery.OrderDeliveredV1;
import fd.delivery.OrderPickedUpV1;
import fd.order.OrderCancelledV1;
import fd.payment.*;
import fd.restaurant.OrderReadyForPickupV1;
import fd.restaurant.RestaurantAcceptedV1;
import fd.restaurant.RestaurantRejectedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaListener {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final KafkaTopics topics;
    private final OrderService orderService;

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.cart-checked-out}", groupId = "${kafka.consumer.group-id}")
    public void onCartCheckedOut(CartCheckedOutV1 event) {
        orderService.createOrderFromCart(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.payment-authorized}", groupId = "${kafka.consumer.group-id}")
    public void onPaymentAuthorized(PaymentAuthorizedV1 event) {
        orderService.processPaymentAuthorized(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.restaurant-accepted}", groupId = "${kafka.consumer.group-id}")
    public void onRestaurantAccepted(RestaurantAcceptedV1 event) {
        orderService.processPaymentAccepted(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.payment-failed}", groupId = "${kafka.consumer.group-id}")
    public void onPaymentFailed(PaymentFailedV1 event) {
        orderService.processPaymentFailed(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.restaurant-rejected}", groupId = "${kafka.consumer.group-id}")
    public void onRestaurantRejected(RestaurantRejectedV1 event) {
        orderService.processRestaurantRejected(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.order-ready}", groupId = "${kafka.consumer.group-id}")
    public void onOrderReady(OrderReadyForPickupV1 event) {
        orderService.processOrderReady(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.order-picked-up}", groupId = "${kafka.consumer.group-id}")
    public void onOrderPickedUp(OrderPickedUpV1 event) {
        orderService.processOrderPickedUp(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.order-delivered}", groupId = "${kafka.consumer.group-id}")
    public void onOrderDelivered(OrderDeliveredV1 event) {
        orderService.processOrderDelivered(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.fee-charged}", groupId = "${kafka.consumer.group-id}")
    public void onFeeCharged(FeeChargedV1 event) {
        orderService.processFeeCharged(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.fee-failed}", groupId = "${kafka.consumer.group-id}")
    public void onFeeFailed(FeeFailedV1 event) {
        orderService.processFeeFailed(event);
    }

    @DltHandler
    @Transactional
    public void handleDltPaymentAuthorized(PaymentAuthorizedV1 event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Payment authorized event failed after all retries, requesting refund orderId={} topic={}", event.getOrderId(), topic);

        var order = orderService.findOrder(event.getOrderId());

        var refundEvent = new RefundRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                event.getOrderId(),
                event.getAmountCents(),
                event.getCurrency(),
                "Order processing failed after payment authorization"
        );
        outboxService.publish(topics.getRefundRequested(), event.getOrderId().toString(), refundEvent);

        order.setStatus(OrderStatus.PROCESSING_FAILED);
        orderRepository.save(order);

        log.info("Refund requested for failed order processing orderId={}", event.getOrderId());
    }

    @DltHandler
    @Transactional
    public void handleDltRestaurantAccepted(RestaurantAcceptedV1 event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Restaurant accepted event failed after all retries, cancelling order and requesting refund orderId={} topic={}", event.getOrderId(), topic);

        var order = orderService.findOrder(event.getOrderId());

        var cancelEvent = new OrderCancelledV1(
                UUID.randomUUID(),
                Instant.now(),
                event.getOrderId(),
                order.getCustomerUserId(),
                "Order processing failed after restaurant acceptance"
        );

        var refundEvent = new RefundRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                event.getOrderId(),
                order.getTotalCents(),
                order.getCurrency(),
                "Order processing failed after restaurant acceptance"
        );

        outboxService.publish(topics.getOrderCancelled(), event.getOrderId().toString(), cancelEvent);
        outboxService.publish(topics.getRefundRequested(), event.getOrderId().toString(), refundEvent);

        order.setStatus(OrderStatus.PROCESSING_FAILED);
        orderRepository.save(order);

        log.info("Cancellation and refund requested for failed order orderId={}", event.getOrderId());
    }
}
