package com.example.food.catalog.repo;

import com.example.food.catalog.domain.MenuItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface MenuItemRepository extends JpaRepository<MenuItemEntity, UUID> {}
