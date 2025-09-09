package com.example.food.catalog.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.UUID;

@Entity @Table(name="menu_items")
@Getter @Setter
public class MenuItemEntity {
  @Id private UUID id;
  @Column(nullable=false) private UUID restaurantId;
  private UUID sectionId;
  @Column(nullable=false) private String name;
  private String description;
  @Column(nullable=false) private int priceCents;
  @Column(nullable=false) private boolean available = true;
  @Column(nullable=false) private int version = 0;
}
