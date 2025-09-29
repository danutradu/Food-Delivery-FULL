package com.example.food.catalog.service;

import com.example.food.catalog.config.KafkaTopics;
import com.example.food.catalog.dto.MenuItemUpsert;
import com.example.food.catalog.dto.RestaurantUpsert;
import com.example.food.catalog.exception.MenuItemNotFoundException;
import com.example.food.catalog.exception.RestaurantNotFoundException;
import com.example.food.catalog.model.MenuItemEntity;
import com.example.food.catalog.model.RestaurantEntity;
import com.example.food.catalog.repository.MenuItemRepository;
import com.example.food.catalog.repository.RestaurantRepository;
import com.example.food.catalog.util.CatalogFactory;
import com.example.food.common.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final OutboxService outboxService;
    private final KafkaTopics topics;

    @Transactional
    public RestaurantEntity createRestaurant(RestaurantUpsert req) {
        log.info("RestaurantCreated name={} ownerUserId={}", req.name(), req.ownerUserId());

        var restaurant = CatalogFactory.createRestaurant(req);
        restaurantRepository.save(restaurant);

        var event = CatalogFactory.createRestaurantCreated(restaurant);
        outboxService.publish(topics.getRestaurantCreated(), restaurant.getId().toString(), event);

        log.info("Restaurant created restaurantId={}", restaurant.getId());
        return restaurant;
    }

    public MenuItemEntity upsertMenuItem(UUID restaurantId, MenuItemUpsert req) {
        log.info("MenuItemUpsert restaurantId={} name={}", restaurantId, req.name());

        var menuItem = CatalogFactory.createMenuItem(req);
        menuItem.setRestaurantId(restaurantId);
        menuItemRepository.save(menuItem);

        log.info("Menu item upserted menuItemId={}", menuItem.getId());
        return menuItem;
    }

    public List<RestaurantEntity> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    public RestaurantEntity getRestaurant(UUID id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id.toString()));
    }

    public List<MenuItemEntity> getMenu(UUID restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId);
    }

    public void deleteMenuItem(UUID restaurantId, UUID itemId) {
        log.info("MenuItemDeleted restaurantId={} itemId={}", restaurantId, itemId);

        var menuItem = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new MenuItemNotFoundException(itemId.toString()));

        if (!menuItem.getRestaurantId().equals(restaurantId)) {
            throw new IllegalArgumentException("Menu item does not belong to restaurant");
        }

        menuItemRepository.delete(menuItem);
        log.info("Menu item deleted itemId={}", itemId);
    }

    @Transactional
    public MenuItemEntity setMenuItemAvailability(UUID restaurantId, UUID itemId, boolean available) {
        log.info("MenuItemAvailability restaurantId={} itemId={} available={}", restaurantId, itemId, available);

        var menuItem = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new MenuItemNotFoundException(itemId.toString()));

        if (!menuItem.getRestaurantId().equals(restaurantId)) {
            throw new IllegalArgumentException("Menu item does not belong to restaraunt");
        }

        menuItem.setAvailable(available);
        menuItemRepository.save(menuItem);

        log.info("Published menu item availability event itemId={} available={}", itemId, available);
        return menuItem;
    }

    public RestaurantEntity setRestaurantStatus(UUID restaurantId, boolean open) {
        log.info("RestaurantStatus restaurantId={} open={}", restaurantId, open);

        var restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId.toString()));

        restaurant.setOpen(open);
        restaurantRepository.save(restaurant);

        log.info("Restaurant status updated restaurantId={} open={}", restaurantId, open);
        return restaurant;
    }
}
