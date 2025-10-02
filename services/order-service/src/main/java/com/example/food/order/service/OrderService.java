package com.example.food.order.service;

import com.example.food.common.outbox.OutboxService;
import com.example.food.order.config.KafkaTopics;
import com.example.food.order.dto.OrderResponse;
import com.example.food.order.exception.OrderNotFoundException;
import com.example.food.order.model.OrderEntity;
import com.example.food.order.model.OrderStatus;
import com.example.food.order.model.RefundPolicy;
import com.example.food.order.model.RefundType;
import com.example.food.order.repository.OrderRepository;
import com.example.food.order.util.OrderEventFactory;
import com.example.food.order.util.OrderFactory;
import fd.cart.CartCheckedOutV1;
import fd.delivery.DeliveryRequestedV1;
import fd.delivery.OrderDeliveredV1;
import fd.delivery.OrderPickedUpV1;
import fd.payment.FeeChargedV1;
import fd.payment.FeeFailedV1;
import fd.payment.PaymentAuthorizedV1;
import fd.payment.PaymentFailedV1;
import fd.restaurant.OrderReadyForPickupV1;
import fd.restaurant.RestaurantAcceptanceRequestedV1;
import fd.restaurant.RestaurantAcceptedV1;
import fd.restaurant.RestaurantRejectedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.PAYMENT_AUTHORIZED,
            OrderStatus.RESTAURANT_ACCEPTED,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.OUT_FOR_DELIVERY
    );

    private static final int CANCELLATION_FEE = 5;
    private static final int DELIVERY_FEE = 8;

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final KafkaTopics topics;

    @Transactional
    public void createOrderFromCart(CartCheckedOutV1 event) {
        log.info("Creating order from cart checkout cartId={} customerUserId={}", event.getCartId(), event.getCustomerUserId());

        var existingOrder = orderRepository.findByCartId(event.getCartId());

        if (existingOrder.isPresent()) {
            log.debug("Order already exists for cart checkout cartId={} orderId={}", event.getCartId(), existingOrder.get().getId());
            return;
        }

        var order = OrderFactory.createOrderFromCart(event);
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        var orderCreatedEvent = OrderEventFactory.createOrderCreated(order);
        var paymentRequestedEvent = OrderEventFactory.createPaymentRequested(order);

        outboxService.publish(topics.getOrderCreated(), order.getId().toString(), orderCreatedEvent);
        outboxService.publish(topics.getPaymentRequested(), order.getId().toString(), paymentRequestedEvent);

        log.info("Order created successfully orderId={} total={}", order.getId(), order.getTotal());
    }

    @Transactional
    public void processPaymentAuthorized(PaymentAuthorizedV1 event) {
        log.info("Payment authorized, requesting restaurant acceptance orderId={}", event.getOrderId());

        var order = findOrder(event.getOrderId());

        if (order.getStatus() != OrderStatus.PENDING) {
            log.debug("Order already processed beyond PENDING status orderId={} status={}", order.getId(), order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.PAYMENT_AUTHORIZED);
        orderRepository.save(order);

        var acceptanceRequested = new RestaurantAcceptanceRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                event.getOrderId(),
                order.getRestaurantId(),
                event.getAmount()
        );
        outboxService.publish(topics.getRestaurantAcceptanceRequested(), acceptanceRequested.getOrderId().toString(), acceptanceRequested);

        log.info("Restaurant acceptance requested orderId={}", event.getOrderId());
    }

    @Transactional
    public void processRestaurantAccepted(RestaurantAcceptedV1 event) {
        log.info("Restaurant accepted, requesting delivery orderId={}", event.getOrderId());

        var order = findOrder(event.getOrderId());

        if (order.getStatus() != OrderStatus.PAYMENT_AUTHORIZED) {
            log.debug("Order not in PAYMENT_AUTHORIZED status orderId={} status={}", order.getId(), order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.RESTAURANT_ACCEPTED);
        orderRepository.save(order);

        var deliveryRequested = new DeliveryRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                event.getOrderId(),
                event.getRestaurantId(),
                order.getCustomerUserId()
        );
        outboxService.publish(topics.getDeliveryRequested(), deliveryRequested.getOrderId().toString(), deliveryRequested);

        log.info("Delivery requested orderId={}", event.getOrderId());
    }

    @Transactional
    public void processPaymentFailed(PaymentFailedV1 event) {
        log.warn("Payment failed, marking order as failed orderId={} reason={}", event.getOrderId(), event.getFailureReason());

        var order = findOrder(event.getOrderId());

        if (order.getStatus() != OrderStatus.PENDING) {
            log.debug("Order already processed beyond PENDING status orderId={} status={}", order.getId(), order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);

        log.info("Order marked as payment failed orderId={}", event.getOrderId());
    }

    @Transactional
    public void processRestaurantRejected(RestaurantRejectedV1 event) {
        log.info("Restaurant rejected order orderId={} reason={}", event.getOrderId(), event.getReason());

        var order = findOrder(event.getOrderId());

        if (order.getStatus() != OrderStatus.PAYMENT_AUTHORIZED) {
            log.debug("Order not in PAYMENT_AUTHORIZED status orderId={} status={}", order.getId(), order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.RESTAURANT_REJECTED);
        orderRepository.save(order);

        log.info("Order marked as restaurant rejected orderId={}", event.getOrderId());
    }

    @Transactional
    public void processOrderReady(OrderReadyForPickupV1 event) {
        log.info("Order ready for pickup orderId={}", event.getOrderId());

        var order = findOrder(event.getOrderId());

        if (order.getStatus() != OrderStatus.RESTAURANT_ACCEPTED) {
            log.debug("Order not in RESTAURANT_ACCEPTED status orderId={} status={}", order.getId(), order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        orderRepository.save(order);

        log.info("Order marked ready for pickup orderId={}", event.getOrderId());
    }

    @Transactional
    public void processOrderPickedUp(OrderPickedUpV1 event) {
        log.info("Order picked up by courier orderId={}", event.getOrderId());

        var order = findOrder(event.getOrderId());

        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            log.debug("Order not in READY_FOR_PICKUP status orderId={} status={}", order.getId(), order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(order);

        log.info("Order marked out for delivery orderId={}", event.getOrderId());
    }

    @Transactional
    public void processOrderDelivered(OrderDeliveredV1 event) {
        log.info("Order delivered orderId={}", event.getOrderId());

        var order = findOrder(event.getOrderId());

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            log.debug("Order not in OUT_FOR_DELIVERY status orderId={} status={}", order.getId(), order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        log.info("Order completed - delivered to customer orderId={}", event.getOrderId());
    }

    @Transactional
    public void processFeeCharged(FeeChargedV1 event) {
        log.info("Fee charged successfully orderId={} amount={}", event.getOrderId(), event.getFee());

        var order = findOrder(event.getOrderId());

        if (order.getStatus() == OrderStatus.DELIVERED) {
            log.warn("Cannot cancel delivered order, fee charged as penalty orderId={}", order.getId());
            return;
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.debug("Order already cancelled orderId={}", order.getId());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        var cancelledEvent = OrderEventFactory.createOrderCancelled(order, "Cancelled with fee charged");
        outboxService.publish(topics.getOrderCancelled(), cancelledEvent.getOrderId().toString(), cancelledEvent);

        log.info("Order cancellation completed with fee charged orderId={}", event.getOrderId());
    }

    @Transactional
    public void processFeeFailed(FeeFailedV1 event) {
        log.info("Fee charging failed orderId={} reason={}", event.getOrderId(), event.getFailureReason());

        var order = findOrder(event.getOrderId());

        if (order.getStatus() == OrderStatus.DELIVERED) {
            log.warn("Order was delivered during fee processing, cannot cancel orderId={}", order.getId());
            return;
        }

        log.info("Fee charging failed, order remains in {} state orderId={}", order.getStatus(), order.getId());
    }

    public OrderResponse getOrder(UUID orderId, UUID customerUserId) {
        log.info("Getting order orderId={} customerUserId={}", orderId, customerUserId);

        var order = findOrder(orderId);

        if (!order.getCustomerUserId().equals(customerUserId)) {
            throw new OrderNotFoundException(orderId.toString());
        }

        return OrderFactory.createOrderResponse(order);
    }

    public List<OrderResponse> getOrdersByCustomer(UUID customerUserId) {
        log.info("Getting orders for customer customerUserId={}", customerUserId);

        var orders = orderRepository.findByCustomerUserIdOrderByCreatedAtDesc(customerUserId);

        return orders.stream()
                .map(OrderFactory::createOrderResponse)
                .toList();
    }

    public List<OrderEntity> getAllOrders() {
        log.info("Admin getting all orders");

        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void cancelOrder(UUID orderId, String reason) {
        log.info("Cancelling order orderId={} reason={}", orderId, reason);

        var order = findOrder(orderId);

        if (!canBeCancelled(order.getStatus())) {
            throw new IllegalStateException("Order cannot be cancelled in status: " + order.getStatus());
        }

        var refundPolicy = calculateRefundPolicy(order.getStatus());

        if (refundPolicy.refundType() == RefundType.FULL) {
            // Immediate cancellation
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            var cancelledEvent = OrderEventFactory.createOrderCancelled(order, reason);
            outboxService.publish(topics.getOrderCancelled(), cancelledEvent.getOrderId().toString(), cancelledEvent);

            var refundEvent = OrderEventFactory.createRefundRequested(order, refundPolicy);
            outboxService.publish(topics.getRefundRequested(), refundEvent.getOrderId().toString(), refundEvent);

            log.info("Order cancelled immediately with refund orderId={} amount={}", orderId, order.getTotal());
        } else {
            // Late cancellation - wait for fee processing
            order.setStatus(OrderStatus.CANCELLING);
            orderRepository.save(order);

            var feeEvent = OrderEventFactory.createFeeRequested(order, refundPolicy);
            outboxService.publish(topics.getFeeRequested(), feeEvent.getOrderId().toString(), feeEvent);

            log.info("Order cancellation pending fee payment orderId={} fee={}", orderId, refundPolicy.fee());
        }
    }

    private boolean canBeCancelled(OrderStatus status) {
        return CANCELLABLE_STATUSES.contains(status);
    }

    private RefundPolicy calculateRefundPolicy(OrderStatus status) {
        return switch (status) {
            case PENDING, PAYMENT_AUTHORIZED ->
                    new RefundPolicy(true, RefundType.FULL, 0, "Full refund - order not started");
            case RESTAURANT_ACCEPTED ->
                    new RefundPolicy(false, RefundType.NO_REFUND, CANCELLATION_FEE, "No refund + cancellation fee - courier assigned");
            case READY_FOR_PICKUP ->
                    new RefundPolicy(false, RefundType.NO_REFUND, DELIVERY_FEE, "No refund + delivery status - courier en route");
            case OUT_FOR_DELIVERY ->
                    new RefundPolicy(false, RefundType.NO_REFUND, DELIVERY_FEE, "No refund + delivery fee - courier en route");
            default -> new RefundPolicy(false, RefundType.NO_REFUND, 0, "Cannot cancel in status: " + status);
        };
    }

    public OrderEntity findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));
    }
}
