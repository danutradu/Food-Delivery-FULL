package com.example.food.order.util;

import com.example.food.order.dto.CreateOrderRequest;
import com.example.food.order.dto.OrderResponse;
import com.example.food.order.model.OrderEntity;
import com.example.food.order.model.OrderItemEntity;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class OrderFactory {

    public OrderEntity createOrder(CreateOrderRequest req, UUID customerUserId) {
        var order = new OrderEntity();
        order.setId(UUID.randomUUID());
        order.setCustomerUserId(customerUserId);
        order.setRestaurantId(req.restaurantId());
        order.setCurrency(req.currency());
        order.setCreatedAt(Instant.now());

        var items = req.items().stream()
                .map(item -> createOrderItem(item, order))
                .toList();
        order.setItems(items);

        int total = items.stream()
                .mapToInt(item -> item.getUnitPriceCents() * item.getQuantity())
                .sum();
        order.setTotalCents(total);

        return order;
    }

    private OrderItemEntity createOrderItem(CreateOrderRequest.Item item, OrderEntity order) {
        var orderItem = new OrderItemEntity();
        orderItem.setId(UUID.randomUUID());
        orderItem.setMenuItemId(item.menuItemId());
        orderItem.setName(item.name());
        orderItem.setUnitPriceCents(item.unitPriceCents());
        orderItem.setQuantity(item.quantity());
        orderItem.setOrder(order);
        return orderItem;
    }

    public OrderResponse createOrderResponse(OrderEntity order) {
        var items = order.getItems().stream()
                .map(item -> new OrderResponse.Item(
                        item.getMenuItemId(),
                        item.getName(),
                        item.getUnitPriceCents(),
                        item.getQuantity()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getTotalCents(),
                order.getCurrency(),
                order.getStatus(),
                items
        );
    }
}
