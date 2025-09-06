package com.example.food.order.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.UUID;

@Entity @Table(name="order_items")
@Getter @Setter
public class OrderItemEntity {
  @Id private UUID id;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id")
  private OrderEntity order;
  @Column(nullable=false) private UUID menuItemId;
  @Column(nullable=false) private String name;
  @Column(nullable=false) private int unitPriceCents;
  @Column(nullable=false) private int quantity;
}
