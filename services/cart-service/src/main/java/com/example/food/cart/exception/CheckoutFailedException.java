package com.example.food.cart.exception;

public class CheckoutFailedException extends RuntimeException {
    public CheckoutFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
