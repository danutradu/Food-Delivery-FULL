package com.example.food.ops.repo;

import com.example.food.ops.domain.KitchenTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface KitchenTicketRepository extends JpaRepository<KitchenTicketEntity, UUID> {
  Optional<KitchenTicketEntity> findByOrderId(UUID orderId);
}
