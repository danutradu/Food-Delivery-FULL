package com.example.food.ops.exception;

public class KitchenTicketNotFoundException extends RuntimeException {
    public KitchenTicketNotFoundException(String message) {
        super(message);
    }
}
