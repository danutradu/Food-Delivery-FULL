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
                        item.getUnitPriceCents(),
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
                .setTotalCents(order.getTotalCents())
                .setCurrency(order.getCurrency())
                .build();
    }

    public PaymentRequestedV1 createPaymentRequested(OrderEntity order) {
        return new PaymentRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                order.getId(),
                order.getTotalCents(),
                order.getCurrency()
        );
    }

    public RefundRequestedV1 createRefundRequested(OrderEntity order, RefundPolicy refundPolicy) {
        int refundAmount = refundPolicy.refundType() == RefundType.FULL ? order.getTotalCents() : 0;

        return new RefundRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                order.getId(),
                refundAmount,
                order.getCurrency(),
                refundPolicy.reason()
        );
    }

    public FeeRequestedV1 createFeeRequested(OrderEntity order, RefundPolicy refundPolicy) {
        return new FeeRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                order.getId(),
                refundPolicy.feeCents(),
                order.getCurrency(),
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
