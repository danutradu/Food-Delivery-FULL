package com.example.food.payment.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="payments")
@Getter @Setter
public class PaymentEntity {
  @Id private UUID id;
  @Column(nullable=false, unique=true) private UUID orderId;
  @Column(nullable=false) private int amountCents;
  @Column(nullable=false) private String currency;
  @Column(nullable=false) private String status; // AUTHORIZED, DECLINED
  @Column(nullable=false) private Instant createdAt = Instant.now();
  private String authorizationCode;
}
