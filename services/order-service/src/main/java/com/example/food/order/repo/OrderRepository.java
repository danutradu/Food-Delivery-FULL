package com.example.food.order.repo;

import com.example.food.order.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {}
