package com.example.food.order.web;

import com.example.food.order.app.OrderAppService;
import com.example.food.order.web.dto.CreateOrderRequest;
import com.example.food.order.web.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController {

  private final OrderAppService app;

  @PreAuthorize("hasRole('CUSTOMER')")
  @PostMapping("/orders")
  public OrderResponse create(@Valid @RequestBody CreateOrderRequest req, Authentication auth) {
    UUID userId = UUID.fromString(((Jwt)auth.getPrincipal()).getSubject());
    UUID orderId = app.createOrder(userId, req);
    var items = req.items().stream().map(i -> new OrderResponse.Item(i.menuItemId(), i.name(), i.unitPriceCents(), i.quantity())).toList();
    int total = req.items().stream().mapToInt(i -> i.unitPriceCents() * i.quantity()).sum();
    return new OrderResponse(orderId, total, req.currency(), items);
  }
}
