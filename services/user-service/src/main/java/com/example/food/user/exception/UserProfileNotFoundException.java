package com.example.food.user.exception;

public class UserProfileNotFoundException extends RuntimeException {
    public UserProfileNotFoundException(String message) {
        super(message);
    }
}
