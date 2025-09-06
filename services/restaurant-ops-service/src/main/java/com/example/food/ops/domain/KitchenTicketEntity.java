package com.example.food.ops.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

import java.util.UUID;

@Entity @Table(name="kitchen_tickets")
@Getter @Setter
public class KitchenTicketEntity {
  @Id private UUID id;
  @Column(nullable=false) private UUID orderId;
  @Column(nullable=false) private UUID restaurantId;
  @Column(nullable=false) private String status; // PENDING, ACCEPTED, REJECTED, READY
}
