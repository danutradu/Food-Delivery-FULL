package com.example.food.catalog.util;

import com.example.food.catalog.dto.MenuItemUpsert;
import com.example.food.catalog.dto.RestaurantUpsert;
import com.example.food.catalog.model.MenuItemEntity;
import com.example.food.catalog.model.RestaurantEntity;
import fd.catalog.MenuItemUpdatedV1;
import fd.catalog.RestaurantCreatedV1;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class CatalogFactory {

    public RestaurantEntity createRestaurant(RestaurantUpsert req) {
        var restaurant = new RestaurantEntity();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName(req.name());
        restaurant.setAddress(req.address());
        restaurant.setOwnerUserId(req.ownerUserId());
        restaurant.setOpen(req.isOpen());
        return restaurant;
    }

    public RestaurantCreatedV1 createRestaurantCreated(RestaurantEntity restaurant) {
        return new RestaurantCreatedV1(
                UUID.randomUUID(),
                Instant.now(),
                restaurant.getId(),
                restaurant.getOwnerUserId(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.isOpen()
        );
    }

    public MenuItemEntity createMenuItem(MenuItemUpsert req) {
        var menuItem = new MenuItemEntity();
        menuItem.setId(UUID.randomUUID());
        menuItem.setName(req.name());
        menuItem.setDescription(req.description());
        menuItem.setPriceCents(req.priceCents());
        menuItem.setSectionId(req.sectionId());
        menuItem.setAvailable(req.available());
        return menuItem;
    }

    public MenuItemUpdatedV1 createMenuItemUpdated(MenuItemEntity menuItem) {
        return MenuItemUpdatedV1.newBuilder()
                .setEventId(UUID.randomUUID())
                .setOccurredAt(Instant.now())
                .setRestaurantId(menuItem.getRestaurantId())
                .setMenuItemId(menuItem.getId())
                .setName(menuItem.getName())
                .setDescription(menuItem.getDescription())
                .setPriceCents(menuItem.getPriceCents())
                .setSectionId(menuItem.getSectionId())
                .setAvailable(menuItem.isAvailable())
                .build();
    }
}
