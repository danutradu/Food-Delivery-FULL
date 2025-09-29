package com.example.food.order.service;

import com.example.food.common.outbox.OutboxService;
import com.example.food.order.config.KafkaTopics;
import com.example.food.order.dto.CreateOrderRequest;
import com.example.food.order.dto.OrderResponse;
import com.example.food.order.exception.OrderNotFoundException;
import com.example.food.order.model.OrderStatus;
import com.example.food.order.model.RefundPolicy;
import com.example.food.order.model.RefundType;
import com.example.food.order.repository.OrderRepository;
import com.example.food.order.util.OrderEventFactory;
import com.example.food.order.util.OrderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final int CANCELLATION_FEE_CENTS = 500;
    private static final int DELIVERY_FEE_CENTS = 800;

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final KafkaTopics topics;

    @Transactional
    public OrderResponse createOrder(UUID customerUserId, CreateOrderRequest req) {
        log.info("Creating order customerUserId={} items={} currency={}", customerUserId, req.items().size(), req.currency());

        var order = OrderFactory.createOrder(req, customerUserId);
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        var orderCreatedEvent = OrderEventFactory.createOrderCreated(order);
        var paymentRequestedEvent = OrderEventFactory.createPaymentRequested(order);

        outboxService.publish(topics.getOrderCreated(), order.getId().toString(), orderCreatedEvent);
        outboxService.publish(topics.getPaymentRequested(), order.getId().toString(), paymentRequestedEvent);

        log.info("Order created successfully orderId={}", order.getId());
        return OrderFactory.createOrderResponse(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId, String reason) {
        log.info("Cancelling order orderId={} reason={}", orderId, reason);

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));

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

            log.info("Order cancelled immediately with refund orderId={} amount={}{}", orderId, order.getTotalCents(), order.getCurrency());
        } else {
            // Late cancellation - wait for fee processing
            order.setStatus(OrderStatus.CANCELLING);
            orderRepository.save(order);

            var feeEvent = OrderEventFactory.createFeeRequested(order, refundPolicy);
            outboxService.publish(topics.getFeeRequested(), feeEvent.getOrderId().toString(), feeEvent);

            log.info("Order cancellation pending fee payment orderId={} fee=${}", orderId, refundPolicy.feeCents());
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
                    new RefundPolicy(false, RefundType.NO_REFUND, CANCELLATION_FEE_CENTS, "No refund + cancellation fee - courier assigned");
            case READY_FOR_PICKUP ->
                    new RefundPolicy(false, RefundType.NO_REFUND, DELIVERY_FEE_CENTS, "No refund + delivery status - courier en route");
            case OUT_FOR_DELIVERY ->
                    new RefundPolicy(false, RefundType.NO_REFUND, DELIVERY_FEE_CENTS, "No refund + delivery fee - courier en route");
            default -> new RefundPolicy(false, RefundType.NO_REFUND, 0, "Cannot cancel in status: " + status);
        };
    }
}
