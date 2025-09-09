package com.example.food.auth.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.*;

@Entity @Table(name="users")
@Getter @Setter
public class UserEntity {
  @Id private UUID id;
  @Column(nullable=false, unique=true, length=100) private String username;
  @Column(nullable=false, unique=true, length=255) private String email;
  @Column(nullable=false) private String passwordHash;
  @Column(nullable=false) private boolean enabled = true;

  @ManyToMany(fetch=FetchType.EAGER)
  @JoinTable(name="user_roles",
      joinColumns=@JoinColumn(name="user_id"),
      inverseJoinColumns=@JoinColumn(name="role_id"))
  private Set<RoleEntity> roles = new HashSet<>();
}
