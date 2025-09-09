package com.example.food.catalog.repository;

import com.example.food.catalog.model.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface RestaurantRepository extends JpaRepository<RestaurantEntity, UUID> {}
