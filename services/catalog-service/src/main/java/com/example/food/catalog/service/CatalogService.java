package com.example.food.catalog.service;

import com.example.food.catalog.dto.MenuItemUpsert;
import com.example.food.catalog.dto.RestaurantUpsert;
import com.example.food.catalog.exception.MenuItemNotFoundException;
import com.example.food.catalog.exception.RestaurantNotFoundException;
import com.example.food.catalog.mapper.CatalogMapper;
import com.example.food.catalog.model.MenuItemEntity;
import com.example.food.catalog.model.RestaurantEntity;
import com.example.food.catalog.repository.MenuItemRepository;
import com.example.food.catalog.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final KafkaTemplate<String, SpecificRecord> kafka;
    private final CatalogMapper mapper;

    public RestaurantEntity createRestaurant(RestaurantUpsert req) {
        log.info("RestaurantCreated name={} ownerUserId={}", req.name(), req.ownerUserId());

        var restaurant = mapper.toEntity(req);
        restaurantRepository.save(restaurant);

        var event = mapper.toRestaurantCreated(restaurant);
        ProducerRecord<String, SpecificRecord> rec = new ProducerRecord<>("fd.catalog.restaurant.created.v1", restaurant.getId().toString(), event);
        rec.headers().add("eventType", "fd.catalog.RestaurantCreatedV1".getBytes());
        rec.headers().add("eventId", event.getEventId().toString().getBytes());

        kafka.send(rec).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish restaurant created event restaurantId={}", restaurant.getId(), ex);
            } else {
                log.info("Published restaurant created event restaurantId={}", restaurant.getId());
            }
        });

        return restaurant;
    }

    public MenuItemEntity upsertMenuItem(UUID restaurantId, MenuItemUpsert req) {
        log.info("MenuItemUpsert restaurantId={} name={}", restaurantId, req.name());

        var menuItem = mapper.toEntity(req);
        menuItem.setRestaurantId(restaurantId);
        menuItemRepository.save(menuItem);

        var event = mapper.toMenuItemUpdated(menuItem);
        ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>("fd.catalog.menu-item.updated.v1", menuItem.getId().toString(), event);
        record.headers().add("eventType", "fd.catalog.MenuItemUpdatedV1".getBytes());
        record.headers().add("eventId", event.getEventId().toString().getBytes());

        kafka.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish menu item updated event menuItemId={}", menuItem.getId(), ex);
            } else {
                log.info("Published menu item updated event menuItemId={}", menuItem.getId());
            }
        });

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

    public MenuItemEntity setMenuItemAvailability(UUID restaurantId, UUID itemId, boolean available) {
        log.info("MenuItemAvailability restaurantId={} itemId={} available={}", restaurantId, itemId, available);

        var menuItem = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new MenuItemNotFoundException(itemId.toString()));

        if (!menuItem.getRestaurantId().equals(restaurantId)) {
            throw new IllegalArgumentException("Menu item does not belong to restaraunt");
        }

        menuItem.setAvailable(available);
        menuItemRepository.save(menuItem);

        var event = mapper.toMenuItemUpdated(menuItem);
        ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>("fd.catalog.menu-item.updated.v1", itemId.toString(), event);
        record.headers().add("eventType", "fd.catalog.MenuItemUpdatedV1".getBytes());
        record.headers().add("eventId", event.getEventId().toString().getBytes());

        kafka.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish menu item availability event itemId={}", itemId, ex);
            } else {
                log.info("Published menu item availability event itemId={} available={}", itemId, available);
            }
        });

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
