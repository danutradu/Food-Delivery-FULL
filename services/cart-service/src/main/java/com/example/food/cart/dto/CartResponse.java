package com.example.food.cart.dto;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        UUID restaurantId,
        String currency,
        int totalCents,
        List<CartItemResponse> items
) {
    public record CartItemResponse(UUID itemId, UUID menuItemId, String name, int unitPriceCents, int quantity) {}
}
