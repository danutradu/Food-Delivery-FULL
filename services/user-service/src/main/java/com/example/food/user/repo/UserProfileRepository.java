package com.example.food.user.repo;

import com.example.food.user.domain.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {}
