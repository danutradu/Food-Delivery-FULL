package com.example.food.catalog.controller;

import com.example.food.catalog.dto.MenuItemUpsert;
import com.example.food.catalog.dto.RestaurantUpsert;
import com.example.food.catalog.model.MenuItemEntity;
import com.example.food.catalog.model.RestaurantEntity;
import com.example.food.catalog.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CatalogController {

    private final CatalogService catalogService;

    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    @PostMapping("/restaurants")
    public RestaurantEntity createRestaurant(@Valid @RequestBody RestaurantUpsert req) {
        return catalogService.createRestaurant(req);
    }

    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    @PostMapping("/restaurants/{id}/menu/items")
    public MenuItemEntity upsertMenuItem(@PathVariable("id") UUID restaurantId, @Valid @RequestBody MenuItemUpsert req) {
        return catalogService.upsertMenuItem(restaurantId, req);
    }

    @GetMapping("/restaurants")
    public List<RestaurantEntity> getRestaurants() {
        return catalogService.getAllRestaurants();
    }

    @GetMapping("/restaurants/{id}")
    public RestaurantEntity getRestaurant(@PathVariable UUID id) {
        return catalogService.getRestaurant(id);
    }

    @GetMapping("/restaurants/{id}/menu")
    public List<MenuItemEntity> getMenu(@PathVariable UUID id) {
        return catalogService.getMenu(id);
    }

    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @DeleteMapping("/restaurants/{restaurantId}/menu/items/{itemId}")
    public void deleteMenuItem(@PathVariable UUID restaurantId, @PathVariable UUID itemId) {
        catalogService.deleteMenuItem(restaurantId, itemId);
    }

    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @DeleteMapping("/restaurants/{restaurantId}/menu/items/{itemId}/availability")
    public MenuItemEntity toggleAvailability(@PathVariable UUID restaurantId, @PathVariable UUID itemId, @RequestParam boolean available) {
        return catalogService.setMenuItemAvailability(restaurantId, itemId, available);
    }

    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @DeleteMapping("/restaurants/{id}/status")
    public RestaurantEntity setRestaurantStatus(@PathVariable UUID id, @RequestParam boolean open) {
        return catalogService.setRestaurantStatus(id, open);
    }
}
