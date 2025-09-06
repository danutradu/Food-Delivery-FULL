package com.example.food.cart.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddItemRequest(@NotNull UUID restaurantId, @NotNull UUID menuItemId, String name, int unitPriceCents, int quantity) {}
