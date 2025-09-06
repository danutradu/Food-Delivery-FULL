package com.example.food.order.web.dto;

import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID orderId, int totalCents, String currency, List<Item> items) {
  public record Item(UUID menuItemId, String name, int unitPriceCents, int quantity) {}
}
