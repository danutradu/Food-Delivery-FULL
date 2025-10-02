package com.example.food.cart.dto;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        UUID restaurantId,
        int total,
        List<CartItemResponse> items
) {
    public record CartItemResponse(UUID itemId, UUID menuItemId, String name, int unitPrice, int quantity) {}
}
