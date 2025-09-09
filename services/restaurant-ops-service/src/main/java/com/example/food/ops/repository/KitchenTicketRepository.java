package com.example.food.ops.repository;

import com.example.food.ops.model.KitchenTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface KitchenTicketRepository extends JpaRepository<KitchenTicketEntity, UUID> {
  Optional<KitchenTicketEntity> findByOrderId(UUID orderId);
}
