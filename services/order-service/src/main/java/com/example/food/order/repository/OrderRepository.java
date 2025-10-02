package com.example.food.order.repository;

import com.example.food.order.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByCartId(UUID cartId);
    List<OrderEntity> findByCustomerUserIdOrderByCreatedAtDesc(UUID customerUserId);
    List<OrderEntity> findAllByOrderByCreatedAtDesc();
}
