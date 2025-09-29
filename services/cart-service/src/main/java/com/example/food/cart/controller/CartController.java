package com.example.food.cart.controller;

import com.example.food.cart.dto.AddItemRequest;
import com.example.food.cart.model.CartEntity;
import com.example.food.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/carts/{cartId}/items")
    public CartEntity addItem(@PathVariable("cartId") UUID cartId, @Valid @RequestBody AddItemRequest req, Authentication auth) {
        var customerUserId = UUID.fromString(((Jwt) auth.getPrincipal()).getSubject());
        return cartService.addItem(cartId, req, customerUserId);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/carts/{cartId}/checkout")
    public ResponseEntity<Void> checkout(@PathVariable("cartId") UUID cartId) {
        cartService.checkout(cartId);
        return ResponseEntity.noContent().build();
    }
}
