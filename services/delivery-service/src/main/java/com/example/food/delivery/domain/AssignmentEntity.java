package com.example.food.delivery.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="assignments")
@Getter @Setter
public class AssignmentEntity {
  @Id private UUID id;
  @Column(nullable=false, unique=true) private UUID orderId;
  @Column(nullable=false) private UUID courierId;
  @Column(nullable=false) private String status; // ASSIGNED, PICKED_UP, DELIVERED
  @Column(nullable=false) private Instant assignedAt = Instant.now();
}
