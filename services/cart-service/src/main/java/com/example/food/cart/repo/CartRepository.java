package com.example.food.cart.repo;

import com.example.food.cart.domain.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface CartRepository extends JpaRepository<CartEntity, UUID> {}
