package com.example.food.order.util;

import com.example.food.order.dto.OrderResponse;
import com.example.food.order.model.OrderEntity;
import com.example.food.order.model.OrderItemEntity;
import fd.cart.CartCheckedOutV1;
import fd.cart.CartItem;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class OrderFactory {

    public OrderEntity createOrderFromCart(CartCheckedOutV1 event) {
        var order = new OrderEntity();
        order.setId(UUID.randomUUID());
        order.setCartId(event.getCartId());
        order.setCustomerUserId(event.getCustomerUserId());
        order.setRestaurantId(event.getRestaurantId());
        order.setCurrency(event.getCurrency());
        order.setTotalCents(event.getTotalCents());

        var items = event.getItems().stream()
                .map(item -> createOrderItemFromCart(item, order))
                .toList();
        order.setItems(items);

        return order;
    }

    private OrderItemEntity createOrderItemFromCart(CartItem cartItem, OrderEntity order) {
        var orderItem = new OrderItemEntity();
        orderItem.setId(UUID.randomUUID());
        orderItem.setMenuItemId(cartItem.getMenuItemId());
        orderItem.setName(cartItem.getName());
        orderItem.setUnitPriceCents(cartItem.getUnitPriceCents());
        orderItem.setQuantity(cartItem.getQuantity());
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
