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
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;
    private final KafkaTemplate<String, Object> kafka;
    private final CatalogMapper mapper;

    @Transactional
    public RestaurantEntity createRestaurant(RestaurantUpsert req) {
        log.info("RestaurantCreated name={} ownerUserId={}", req.name(), req.ownerUserId());

        var restaurant = mapper.toEntity(req);
        restaurants.save(restaurant);

        var event = mapper.toRestaurantCreated(restaurant);
        ProducerRecord<String, Object> rec = new ProducerRecord<>("fd.catalog.restaurant.created.v1", restaurant.getId().toString(), event);
        rec.headers().add("eventType", "fd.catalog.RestaurantCreatedV1".getBytes());
        kafka.send(rec);

        return restaurant;
    }

    @Transactional
    public MenuItemEntity upsertMenuItem(UUID restaurantId, MenuItemUpsert req) {
        log.info("MenuItemUpsert restaurantId={} name={}", restaurantId, req.name());

        var menuItem = mapper.toEntity(req);
        menuItem.setRestaurantId(restaurantId);
        menuItems.save(menuItem);

        var event = mapper.toMenuItemUpdated(menuItem);
        ProducerRecord<String, Object> rec = new ProducerRecord<>("fd.catalog.menu-item.updated.v1", menuItem.getId().toString(), event);
        rec.headers().add("eventType", "fd.catalog.MenuItemUpdatedV1".getBytes());
        kafka.send(rec);

        return menuItem;
    }
}
