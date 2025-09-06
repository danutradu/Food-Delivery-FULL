package com.example.food.delivery.repo;

import com.example.food.delivery.domain.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<AssignmentEntity, UUID> {
  Optional<AssignmentEntity> findByOrderId(UUID orderId);
}
