package com.example.food.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddItemRequest(@NotNull UUID restaurantId, @NotNull UUID menuItemId, String name, int unitPriceCents, @Min(1) int quantity) {}
