package com.example.food.cart.exception;

import java.util.UUID;

public class CartNotFoundForUserException extends RuntimeException {
    public CartNotFoundForUserException(UUID customerUserId) {
        super("No cart found for user: " + customerUserId);
    }
}
