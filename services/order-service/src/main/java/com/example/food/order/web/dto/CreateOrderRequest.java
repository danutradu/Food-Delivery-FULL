package com.example.food.order.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(@NotNull UUID restaurantId, @NotBlank String currency, @NotNull List<Item> items) {
  public record Item(@NotNull UUID menuItemId, @NotBlank String name, int unitPriceCents, int quantity) {}
}
