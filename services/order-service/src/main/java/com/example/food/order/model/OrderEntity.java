package com.example.food.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name="orders")
@Getter @Setter
public class OrderEntity {
  @Id private UUID id;
  @Column(nullable=false) private UUID customerUserId;
  @Column(nullable=false) private UUID restaurantId;
  @Column(nullable=false) private int totalCents;
  @Column(nullable=false) private String currency;
  @Column(nullable=false) private Instant createdAt = Instant.now();
  @OneToMany(mappedBy="order", cascade=CascadeType.ALL, orphanRemoval = true, fetch=FetchType.LAZY)
  private List<OrderItemEntity> items = new ArrayList<>();
}
