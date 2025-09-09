package com.example.food.catalog.mapper;

import com.example.food.catalog.dto.MenuItemUpsert;
import com.example.food.catalog.dto.RestaurantUpsert;
import com.example.food.catalog.model.MenuItemEntity;
import com.example.food.catalog.model.RestaurantEntity;
import fd.catalog.MenuItemUpdatedV1;
import fd.catalog.RestaurantCreatedV1;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class}, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CatalogMapper {
  @Mapping(target="id", expression="java(UUID.randomUUID())")
  @Mapping(target="open", source="isOpen")
  RestaurantEntity toEntity(RestaurantUpsert req);

  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="restaurantId", expression="java(r.getId())")
  @Mapping(target="ownerUserId", expression="java(r.getOwnerUserId())")
  @Mapping(target="isOpen", source="open")
  RestaurantCreatedV1 toRestaurantCreated(RestaurantEntity r);

  @Mapping(target="id", expression="java(UUID.randomUUID())")
  MenuItemEntity toEntity(MenuItemUpsert req);

  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="restaurantId", expression="java(m.getRestaurantId())")
  @Mapping(target="menuItemId", expression="java(m.getId())")
  @Mapping(target="sectionId", expression="java(m.getSectionId())")
  MenuItemUpdatedV1 toMenuItemUpdated(MenuItemEntity m);
}
