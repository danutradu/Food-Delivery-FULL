package com.example.food.cart.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CartNotFoundForUserException.class)
    public ResponseEntity<Void> handleCartNotFoundForUserException(CartNotFoundForUserException e) {
        log.warn(e.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<Void> handleCartItemNotFoundException(CartItemNotFoundException e) {
        log.warn(e.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(RestaurantMismatchException.class)
    public ResponseEntity<Void> handleRestaurantMismatchException(RestaurantMismatchException e) {
        log.warn(e.getMessage());
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<Void> handleEmptyCartException(EmptyCartException e) {
        log.warn(e.getMessage());
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(MenuItemNotFoundException.class)
    public ResponseEntity<Void> handleMenuItemNotFoundException(MenuItemNotFoundException e) {
        log.warn(e.getMessage());
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    }
}
