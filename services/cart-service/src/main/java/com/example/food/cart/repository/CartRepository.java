package com.example.food.cart.repository;

import com.example.food.cart.model.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
public interface CartRepository extends JpaRepository<CartEntity, UUID> {
    Optional<CartEntity> findByCustomerUserId(UUID customerUserId);
}
