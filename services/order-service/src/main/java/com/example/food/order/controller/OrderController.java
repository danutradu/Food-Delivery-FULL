package com.example.food.order.controller;

import com.example.food.order.service.OrderService;
import com.example.food.order.dto.CreateOrderRequest;
import com.example.food.order.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderController {

  private final OrderService orderService;

  @PreAuthorize("hasRole('CUSTOMER')")
  @PostMapping("/orders")
  public OrderResponse create(@Valid @RequestBody CreateOrderRequest req, Authentication auth) {
    var userId = UUID.fromString(((Jwt)auth.getPrincipal()).getSubject());
    return orderService.createOrderWithResponse(userId, req);
  }
}
