package com.example.food.cart.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.UUID;

@Entity @Table(name="cart_items")
@Getter @Setter
public class CartItemEntity {
  @Id private UUID id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="cart_id")
  private CartEntity cart;
  @Column(nullable=false) private UUID menuItemId;
  @Column(nullable=false) private String name;
  @Column(nullable=false) private int unitPrice;
  @Column(nullable=false) private int quantity;
}
