package com.example.food.cart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MenuItemCacheService {

    private final Map<String, MenuItemDto> cache = new ConcurrentHashMap<>();

    public MenuItemDto getMenuItem(UUID restaurantId, UUID menuItemId) {
        var key = createKey(restaurantId, menuItemId);
        var item = cache.get(key);

        if (item == null) {
            log.warn("Menu item not found in cache restaurantId={} menuItemId={}", restaurantId, menuItemId);
        }

        return item;
    }

    public void updateMenuItem(UUID restaurantId, UUID menuItemId, String name, int priceCents, String currency, boolean available) {
        var key = createKey(restaurantId, menuItemId);
        var item = new MenuItemDto(menuItemId, name, priceCents, currency, available);
        cache.put(key, item);
        log.info("Updated menu item cache restaurantId={} menuItemId={} name={} available={}", restaurantId, menuItemId, name, available);
    }

    public void removeMenuItem(UUID restaurantId, UUID menuItemId) {
        var key = createKey(restaurantId, menuItemId);
        cache.remove(key);
        log.info("Removed menu item from cache restaurantId={} menuItemId={}", restaurantId, menuItemId);
    }

    private String createKey(UUID restaurantId, UUID menuItemId) {
        return restaurantId + "_" + menuItemId;
    }

    public record MenuItemDto(UUID id, String name, int priceCents, String currency, boolean available) {}
}
