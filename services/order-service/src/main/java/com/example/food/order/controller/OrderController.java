package com.example.food.order.controller;

import com.example.food.order.dto.OrderResponse;
import com.example.food.order.model.OrderEntity;
import com.example.food.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrder(@PathVariable UUID orderId, Authentication auth) {
        var userId = UUID.fromString(((Jwt) auth.getPrincipal()).getSubject());
        return orderService.getOrder(userId, userId);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/orders")
    public List<OrderResponse> getOrders(Authentication auth) {
        var userId = UUID.fromString(((Jwt) auth.getPrincipal()).getSubject());
        return orderService.getOrdersByCustomer(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/orders")
    public List<OrderEntity> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/orders/{orderId}/cancellation")
    public void cancel(@PathVariable UUID orderId, @RequestParam(defaultValue = "Customer requested") String reason) {
        orderService.cancelOrder(orderId, reason);
    }
}
