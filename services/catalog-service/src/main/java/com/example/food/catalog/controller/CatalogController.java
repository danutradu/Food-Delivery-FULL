package com.example.food.catalog.controller;

import com.example.food.catalog.model.MenuItemEntity;
import com.example.food.catalog.model.RestaurantEntity;
import com.example.food.catalog.repository.MenuItemRepository;
import com.example.food.catalog.repository.RestaurantRepository;
import com.example.food.catalog.dto.MenuItemUpsert;
import com.example.food.catalog.dto.RestaurantUpsert;
import com.example.food.catalog.mapper.CatalogMapper;
import fd.catalog.MenuItemUpdatedV1;
import fd.catalog.RestaurantCreatedV1;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CatalogController {

  private final RestaurantRepository restaurants;
  private final MenuItemRepository menuItems;
  private final KafkaTemplate<String, Object> kafka;
  private final CatalogMapper mapper;

  @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
  @PostMapping("/restaurants")
  public RestaurantEntity createRestaurant(@Valid @RequestBody RestaurantUpsert req) {
    log.info("RestaurantCreated name={} ownerUserId={}", req.name(), req.ownerUserId());
    RestaurantEntity r = mapper.toEntity(req);
    restaurants.save(r);
    RestaurantCreatedV1 evt = mapper.toRestaurantCreated(r);
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.catalog.restaurant.created.v1", r.getId().toString(), evt);
    rec.headers().add("eventType","fd.catalog.RestaurantCreatedV1".getBytes());
    kafka.send(rec);
    return r;
  }

  @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
  @PostMapping("/restaurants/{id}/menu/items")
  public MenuItemEntity upsertMenuItem(@PathVariable("id") UUID restaurantId, @Valid @RequestBody MenuItemUpsert req) {
    log.info("MenuItemUpsert restaurantId={} name={}", restaurantId, req.name());
    MenuItemEntity m = mapper.toEntity(req);
    m.setRestaurantId(restaurantId);
    menuItems.save(m);
    MenuItemUpdatedV1 evt = mapper.toMenuItemUpdated(m);
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.catalog.menu-item.updated.v1", m.getId().toString(), evt);
    rec.headers().add("eventType","fd.catalog.MenuItemUpdatedV1".getBytes());
    kafka.send(rec);
    return m;
  }
}
