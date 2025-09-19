package com.example.food.catalog.repository;

import com.example.food.catalog.model.MenuItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItemEntity, UUID> {
    List<MenuItemEntity> findByRestaurantId(UUID restaurantId);
}
