package com.example.food.cart.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.*;

@Entity @Table(name="carts")
@Getter @Setter
public class CartEntity {
  @Id private UUID id;
  @Column(nullable=false) private UUID customerUserId;
  @OneToMany(mappedBy="cart", cascade=CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private List<CartItemEntity> items = new ArrayList<>();
  @Column(nullable=false) private String currency = "USD";
}
