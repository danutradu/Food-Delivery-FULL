package com.example.food.catalog.repo;

import com.example.food.catalog.domain.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface RestaurantRepository extends JpaRepository<RestaurantEntity, UUID> {}
