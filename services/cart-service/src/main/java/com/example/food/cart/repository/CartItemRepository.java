package com.example.food.cart.repository;

import com.example.food.cart.model.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {

    @Modifying
    @Query("UPDATE CartItemEntity i SET i.name = :name, i.unitPriceCents = :price WHERE i.menuItemId = :menuItemId")
    int updateByMenuItemId(@Param("menuItemId") UUID menuItemId,
                           @Param("name") String name,
                           @Param("price") int priceCents);
}
