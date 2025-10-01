package com.example.food.cart.controller;

import com.example.food.cart.dto.AddItemRequest;
import com.example.food.cart.dto.CartResponse;
import com.example.food.cart.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    private UUID getUserId(Authentication auth) {
        return UUID.fromString(((Jwt) auth.getPrincipal()).getSubject());
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/cart/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddItemRequest req, Authentication auth) {
        var cart = cartService.addItem(req, getUserId(auth));
        return ResponseEntity.ok(cart);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/cart")
    public ResponseEntity<CartResponse> getCart(Authentication auth) {
        var cart = cartService.getCart(getUserId(auth));
        return cart != null ? ResponseEntity.ok(cart) : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/cart/items/{itemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@PathVariable UUID itemId, @RequestParam @Min(1) int quantity, Authentication auth) {
        var cart = cartService.updateItemQuantity(getUserId(auth), itemId, quantity);
        return ResponseEntity.ok(cart);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping("/cart/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable UUID itemId, Authentication auth) {
        var cart = cartService.removeItem(getUserId(auth), itemId);
        return ResponseEntity.ok(cart);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping("/cart")
    public ResponseEntity<Void> clearCart(Authentication auth) {
        cartService.clearCart(getUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/cart/checkout")
    public ResponseEntity<Void> checkout(Authentication auth) {
        cartService.checkout(getUserId(auth));
        return ResponseEntity.noContent().build();
    }
}
