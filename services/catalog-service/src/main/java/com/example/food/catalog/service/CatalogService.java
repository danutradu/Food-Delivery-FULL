package com.example.food.catalog.service;

import com.example.food.catalog.dto.MenuItemUpsert;
import com.example.food.catalog.dto.RestaurantUpsert;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;
    private final KafkaTemplate<String, SpecificRecord> kafka;
    private final CatalogMapper mapper;

    public RestaurantEntity createRestaurant(RestaurantUpsert req) {
        log.info("RestaurantCreated name={} ownerUserId={}", req.name(), req.ownerUserId());

        var restaurant = mapper.toEntity(req);
        restaurants.save(restaurant);

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
        menuItems.save(menuItem);

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
}
