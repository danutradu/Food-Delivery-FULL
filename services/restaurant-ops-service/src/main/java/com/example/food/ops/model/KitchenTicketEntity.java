package com.example.food.ops.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="kitchen_tickets")
@Getter @Setter
public class KitchenTicketEntity {
  @Id private UUID id;
  @Column(nullable=false) private UUID orderId;
  @Column(nullable=false) private UUID restaurantId;
  @Column(nullable = false) private UUID customerUserId;
  @Enumerated(EnumType.STRING)
  @Column(nullable=false) private KitchenTicketStatus status = KitchenTicketStatus.PENDING;
  private String specialInstructions;
  private int estimatedPrepTimeMinutes;
  @Column(nullable = false) private Instant receivedAt;
  private Instant acceptedAt;
  private Instant startedAt;
  private Instant readyAt;
}
