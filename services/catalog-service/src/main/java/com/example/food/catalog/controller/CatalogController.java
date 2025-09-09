package com.example.food.catalog.controller;

import com.example.food.catalog.model.MenuItemEntity;
import com.example.food.catalog.model.RestaurantEntity;
import com.example.food.catalog.repository.MenuItemRepository;
import com.example.food.catalog.repository.RestaurantRepository;
import com.example.food.catalog.dto.MenuItemUpsert;
import com.example.food.catalog.dto.RestaurantUpsert;
import com.example.food.catalog.mapper.CatalogMapper;
import com.example.food.catalog.service.CatalogService;
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
}
