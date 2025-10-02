package com.example.food.order.util;

import com.example.food.order.model.OrderEntity;
import com.example.food.order.model.RefundPolicy;
import com.example.food.order.model.RefundType;
import fd.order.OrderCancelledV1;
import fd.order.OrderCreatedV1;
import fd.order.OrderItem;
import fd.payment.FeeRequestedV1;
import fd.payment.PaymentRequestedV1;
import fd.payment.RefundRequestedV1;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class OrderEventFactory {

    public OrderCreatedV1 createOrderCreated(OrderEntity order) {
        var items = order.getItems().stream()
                .map(item -> new OrderItem(
                        item.getMenuItemId(),
                        item.getName(),
                        item.getUnitPrice(),
                        item.getQuantity()
                ))
                .toList();

        return OrderCreatedV1.newBuilder()
                .setEventId(UUID.randomUUID())
                .setOccurredAt(Instant.now())
                .setOrderId(order.getId())
                .setCustomerUserId(order.getCustomerUserId())
                .setRestaurantId(order.getRestaurantId())
                .setItems(items)
                .setTotal(order.getTotal())
                .build();
    }

    public PaymentRequestedV1 createPaymentRequested(OrderEntity order) {
        return new PaymentRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                order.getId(),
                order.getTotal()
        );
    }

    public RefundRequestedV1 createRefundRequested(OrderEntity order, RefundPolicy refundPolicy) {
        int refundAmount = refundPolicy.refundType() == RefundType.FULL ? order.getTotal() : 0;

        return new RefundRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                order.getId(),
                refundAmount,
                refundPolicy.reason()
        );
    }

    public FeeRequestedV1 createFeeRequested(OrderEntity order, RefundPolicy refundPolicy) {
        return new FeeRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                order.getId(),
                refundPolicy.fee(),
                refundPolicy.reason()
        );
    }

    public OrderCancelledV1 createOrderCancelled(OrderEntity order, String reason) {
        return new OrderCancelledV1(
                UUID.randomUUID(),
                Instant.now(),
                order.getId(),
                order.getCustomerUserId(),
                reason
        );
    }
}
