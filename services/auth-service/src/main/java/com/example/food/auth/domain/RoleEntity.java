package com.example.food.auth.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Entity @Table(name="roles")
@Getter @Setter
public class RoleEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false, unique=true, length=50) private String name;
}
