package com.example.food.order.repository;

import com.example.food.order.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {}
