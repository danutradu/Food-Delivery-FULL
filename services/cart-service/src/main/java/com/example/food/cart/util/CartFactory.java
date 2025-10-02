package com.example.food.cart.util;

import com.example.food.cart.dto.CartResponse;
import com.example.food.cart.model.CartEntity;
import com.example.food.cart.model.CartItemEntity;
import fd.cart.CartCheckedOutV1;
import fd.cart.CartItem;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class CartFactory {

    public CartItemEntity createCartItem(UUID menuItemId, String name, int unitPriceCents, int quantity) {
        var item = new CartItemEntity();
        item.setId(UUID.randomUUID());
        item.setMenuItemId(menuItemId);
        item.setName(name);
        item.setUnitPriceCents(unitPriceCents);
        item.setQuantity(quantity);
        return item;
    }

    public CartCheckedOutV1 createCartCheckedOut(CartEntity cart) {
        var items = cart.getItems().stream()
                .map(item -> new CartItem(
                        item.getMenuItemId(),
                        item.getName(),
                        item.getUnitPriceCents(),
                        item.getQuantity()
                ))
                .toList();

        int totalCents = cart.getItems().stream()
                .mapToInt(item -> item.getUnitPriceCents() * item.getQuantity())
                .sum();

        return new CartCheckedOutV1(
                UUID.randomUUID(),
                Instant.now(),
                cart.getId(),
                cart.getCustomerUserId(),
                cart.getRestaurantId(),
                cart.getCurrency(),
                totalCents,
                items
        );
    }

    public CartResponse createCartResponse(CartEntity cart) {
        var items = cart.getItems().stream()
                .map(item -> new CartResponse.CartItemResponse(
                        item.getId(),
                        item.getMenuItemId(),
                        item.getName(),
                        item.getUnitPriceCents(),
                        item.getQuantity()
                ))
                .toList();

        return new CartResponse(
                cart.getId(),
                cart.getRestaurantId(),
                cart.getCurrency(),
                cart.getTotalCents(),
                items
        );
    }
}
