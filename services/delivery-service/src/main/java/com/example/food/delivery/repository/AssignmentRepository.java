package com.example.food.delivery.repository;

import com.example.food.delivery.model.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<AssignmentEntity, UUID> {
  Optional<AssignmentEntity> findByOrderId(UUID orderId);
}
