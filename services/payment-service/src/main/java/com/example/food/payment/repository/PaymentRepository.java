package com.example.food.payment.repository;

import com.example.food.payment.model.PaymentEntity;
import com.example.food.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
  Optional<PaymentEntity> findByOrderId(UUID orderId);
  Optional<PaymentEntity> findByOrderIdAndAndStatus(UUID orderId, PaymentStatus status);
}
