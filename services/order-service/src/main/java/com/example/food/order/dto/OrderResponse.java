package com.example.food.order.dto;

import com.example.food.order.model.OrderStatus;

import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID orderId, int totalCents, String currency, OrderStatus status, List<Item> items) {
  public record Item(UUID menuItemId, String name, int unitPriceCents, int quantity) {}
}
