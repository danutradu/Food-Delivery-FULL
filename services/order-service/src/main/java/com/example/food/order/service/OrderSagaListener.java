package com.example.food.order.service;

import com.example.food.common.outbox.OutboxService;
import com.example.food.order.config.KafkaTopics;
import com.example.food.order.config.StandardRetryableTopic;
import com.example.food.order.dto.CreateOrderRequest;
import com.example.food.order.exception.OrderNotFoundException;
import com.example.food.order.model.OrderEntity;
import com.example.food.order.model.OrderStatus;
import com.example.food.order.model.ProcessedEventEntity;
import com.example.food.order.repository.OrderRepository;
import com.example.food.order.repository.ProcessedEventRepository;
import com.example.food.order.util.OrderEventFactory;
import fd.cart.CartCheckedOutV1;
import fd.delivery.DeliveryRequestedV1;
import fd.delivery.OrderDeliveredV1;
import fd.delivery.OrderPickedUpV1;
import fd.order.OrderCancelledV1;
import fd.payment.*;
import fd.restaurant.OrderReadyForPickupV1;
import fd.restaurant.RestaurantAcceptanceRequestedV1;
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
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxService outboxService;
    private final KafkaTopics topics;
    private final OrderService orderService;

    private boolean isAlreadyProcessed(UUID event) {
        if (processedEventRepository.existsByEventId(event)) {
            log.debug("Event already processed eventId={}", event);
            return true;
        }
        return false;
    }

    private void markAsProcessed(UUID eventId, String eventType) {
        processedEventRepository.save(new ProcessedEventEntity(eventId, eventType));
    }

    private OrderEntity findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.cart-checked-out}", groupId = "${kafka.consumer.group-id}")
    @Transactional
    public void onCartCheckedOut(CartCheckedOutV1 event) {
        log.info("Cart checked out, creating order cartId={} customerUserId={}", event.getCartId(), event.getCustomerUserId());

        // Idempotency check
        if (isAlreadyProcessed(event.getEventId())) return;

        var orderItems = event.getItems().stream()
                .map(item -> new CreateOrderRequest.Item(
                        item.getMenuItemId(),
                        item.getName(),
                        item.getUnitPriceCents(),
                        item.getQuantity()
                ))
                .toList();

        var createOrderRequest = new CreateOrderRequest(
                event.getRestaurantId(),
                event.getCurrency(),
                orderItems
        );

        orderService.createOrder(event.getCustomerUserId(), createOrderRequest);

        markAsProcessed(event.getEventId(), event.getClass().getSimpleName());

        log.info("Order created from cart checkout cartId={}", event.getCartId());
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.payment-authorized}", groupId = "${kafka.consumer.group-id}")
    @Transactional
    public void onPaymentAuthorized(PaymentAuthorizedV1 event) {
        log.info("Payment authorized, requesting restaurant acceptance orderId={}", event.getOrderId());

        if (isAlreadyProcessed(event.getEventId())) return;

        var order = findOrder(event.getOrderId());

        order.setStatus(OrderStatus.PAYMENT_AUTHORIZED);
        orderRepository.save(order);

        var out = new RestaurantAcceptanceRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                event.getOrderId(),
                order.getRestaurantId(),
                event.getAmountCents(),
                event.getCurrency()
        );
        outboxService.publish(topics.getRestaurantAcceptanceRequested(), out.getOrderId().toString(), out);

        markAsProcessed(event.getEventId(), event.getClass().getSimpleName());

        log.info("Processed payment authorized event orderId={}", event.getOrderId());
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.restaurant-accepted}", groupId = "${kafka.consumer.group-id}")
    @Transactional
    public void onRestaurantAccepted(RestaurantAcceptedV1 event) {
        log.info("Restaurant accepted, requesting delivery orderId={}", event.getOrderId());

        if (isAlreadyProcessed(event.getEventId())) return;

        var order = findOrder(event.getOrderId());

        order.setStatus(OrderStatus.RESTAURANT_ACCEPTED);
        orderRepository.save(order);

        var out = new DeliveryRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                event.getOrderId(),
                event.getRestaurantId(),
                order.getCustomerUserId()
        );
        outboxService.publish(topics.getDeliveryRequested(), out.getOrderId().toString(), out);

        markAsProcessed(event.getEventId(), event.getClass().getSimpleName());

        log.info("Processed restaurant accepted event orderId={}", event.getOrderId());
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.payment-failed}", groupId = "${kafka.consumer.group-id}")
    @Transactional
    public void onPaymentFailed(PaymentFailedV1 event) {
        log.warn("Payment failed, cancelling order orderId={} reason={}", event.getOrderId(), event.getFailureReason());

        if (isAlreadyProcessed(event.getEventId())) return;

        var order = findOrder(event.getOrderId());

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);

        markAsProcessed(event.getEventId(), event.getClass().getSimpleName());

        log.info("Order cancelled due to payment failure orderId={}", event.getOrderId());
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.restaurant-rejected}", groupId = "${kafka.consumer.group-id}")
    @Transactional
    public void onRestaurantRejected(RestaurantRejectedV1 event) {
        log.info("Restaurant rejected order orderId={}", event.getOrderId()); // TODO: reason

        if (isAlreadyProcessed(event.getEventId())) return;

        var order = findOrder(event.getOrderId());

        order.setStatus(OrderStatus.RESTAURANT_REJECTED);
        orderRepository.save(order);
        markAsProcessed(event.getEventId(), event.getClass().getSimpleName());

        log.info("Order cancelled due to restaurant rejection orderId={}", event.getOrderId());
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.order-ready}", groupId = "${kafka.consumer.group-id}")
    @Transactional
    public void onOrderReady(OrderReadyForPickupV1 event) {
        log.info("Order ready for pickup orderId={}", event.getOrderId());

        if (isAlreadyProcessed(event.getEventId())) return;

        var order = findOrder(event.getOrderId());

        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        orderRepository.save(order);
        markAsProcessed(event.getEventId(), event.getClass().getSimpleName());

        log.info("Order marked ready for pickup orderId={}", event.getOrderId());
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.order-picked-up}", groupId = "${kafka.consumer.group-id}")
    @Transactional
    public void onOrderPickedUp(OrderPickedUpV1 event) {
        log.info("Order picked up by courier orderId={}", event.getOrderId());

        if (isAlreadyProcessed(event.getEventId())) return;

        var order = findOrder(event.getOrderId());

        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(order);
        markAsProcessed(event.getEventId(), event.getClass().getSimpleName());

        log.info("Order marked out for delivery orderId={}", event.getOrderId());
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.order-delivered}", groupId = "${kafka.consumer.group-id}")
    @Transactional
    public void onOrderDelivered(OrderDeliveredV1 event) {
        log.info("Order delivered orderId={}", event.getOrderId());

        if (isAlreadyProcessed(event.getEventId())) return;

        var order = findOrder(event.getOrderId());

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
        markAsProcessed(event.getEventId(), event.getClass().getSimpleName());

        log.info("Order completed - delivered to customer orderId orderId={}", event.getOrderId());
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.fee-charged}", groupId = "${kafka.consumer.group-id}")
    @Transactional
    public void onFeeCharged(FeeChargedV1 event) {
        log.info("Fee charged successfully orderId={} amount={}", event.getOrderId(), event.getFeeCents());

        if (isAlreadyProcessed(event.getEventId())) return;

        var order = findOrder(event.getOrderId());

        if (order.getStatus() == OrderStatus.DELIVERED) {
            log.warn("Cannot cancel delivered order, fee charged as penalty orderId={}", event.getOrderId());
            markAsProcessed(event.getEventId(), event.getClass().getSimpleName());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        var cancelledEvent = OrderEventFactory.createOrderCancelled(order, "Cancelled with fee charged");
        outboxService.publish(topics.getOrderCancelled(), cancelledEvent.getOrderId().toString(), cancelledEvent);

        markAsProcessed(event.getEventId(), event.getClass().getSimpleName());

        log.info("Order cancellationm completed with fee charged orderId={}", event.getOrderId());
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.fee-failed}", groupId = "${kafka.consumer.group-id}")
    @Transactional
    public void onFeeFailed(FeeFailedV1 event) {
        log.info("Fee charging failed orderId={} reason={}", event.getOrderId(), event.getFailureReason());

        if (isAlreadyProcessed(event.getEventId())) return;

        var order = findOrder(event.getOrderId());

        if (order.getStatus() == OrderStatus.DELIVERED) {
            log.warn("Order was delivered during fee processing, cannot cancel orderId={}", order.getId());
            markAsProcessed(event.getEventId(), event.getClass().getSimpleName());
            return;
        }

        markAsProcessed(event.getEventId(), event.getClass().getSimpleName());

        log.info("Fee charging failed, order remains in {} state orderId={}", order.getStatus(), order.getId());
    }

    @DltHandler
    @Transactional
    public void handleDltPaymentAuthorized(PaymentAuthorizedV1 event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Payment authorized event failed after all retries, requesting refund orderId={} topic={}", event.getOrderId(), topic);

        var order = findOrder(event.getOrderId());

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

        var order = findOrder(event.getOrderId());

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
