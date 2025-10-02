package com.example.food.cart.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
@Getter
@Setter
public class CartEntity {
    @Id
    private UUID id;
    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;
    @Column(nullable = false)
    private UUID customerUserId;
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItemEntity> items = new ArrayList<>();

    public int getTotal() {
        return items.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
    }

    public int getItemCount() {
        return items.stream()
                .mapToInt(CartItemEntity::getQuantity)
                .sum();
    }
}
