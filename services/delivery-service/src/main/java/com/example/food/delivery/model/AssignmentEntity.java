package com.example.food.delivery.model;

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
  @Enumerated(EnumType.STRING)
  @Column(nullable=false) private AssignmentStatus status;
  @Column(nullable=false) private Instant assignedAt = Instant.now();
}
