package com.example.food.cart.exception;

import java.util.UUID;

public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(UUID itemId) {
        super("Item not found in cart: " + itemId);
    }
}
