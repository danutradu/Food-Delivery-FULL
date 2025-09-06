package com.example.food.payment.repo;

import com.example.food.payment.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
  Optional<PaymentEntity> findByOrderId(UUID orderId);
}
