package com.example.food.user.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.UUID;

@Entity @Table(name="users_profile")
@Getter @Setter
public class UserProfileEntity {
  @Id private UUID userId;
  private String username;
  private String email;
}
