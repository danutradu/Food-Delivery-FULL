package com.example.food.delivery.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.UUID;

@Entity @Table(name="couriers")
@Getter @Setter
public class CourierEntity {
  @Id private UUID id;
  @Column(nullable=false) private UUID userId;
  private String vehicle;
  private boolean active = true;
}
