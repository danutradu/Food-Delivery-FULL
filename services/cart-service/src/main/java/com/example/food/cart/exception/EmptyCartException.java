package com.example.food.cart.exception;

import java.util.UUID;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException(UUID customerUserId) {
        super("Cannot checkout empty cart for user: " + customerUserId);
    }
}
