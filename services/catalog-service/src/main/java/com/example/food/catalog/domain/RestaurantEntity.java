package com.example.food.catalog.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.UUID;

@Entity @Table(name="restaurants")
@Getter @Setter
public class RestaurantEntity {
  @Id private UUID id;
  @Column(nullable=false) private UUID ownerUserId;
  @Column(nullable=false) private String name;
  @Column(nullable=false) private String address;
  @Column(nullable=false) private boolean open = true;
}
