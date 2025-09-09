package com.example.food.ops.mapping;

import com.example.food.ops.model.KitchenTicketEntity;
import fd.restaurant.OrderReadyForPickupV1;
import fd.restaurant.RestaurantAcceptedV1;
import fd.restaurant.RestaurantRejectedV1;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class})
public interface OpsMappers {
  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="orderId", expression="java(t.getOrderId())")
  @Mapping(target="restaurantId", expression="java(t.getRestaurantId())")
  @Mapping(target="estimatedReadyMinutes", source="etaMinutes")
  RestaurantAcceptedV1 toAccepted(KitchenTicketEntity t, int etaMinutes);

  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="orderId", expression="java(t.getOrderId())")
  @Mapping(target="restaurantId", expression="java(t.getRestaurantId())")
  @Mapping(target="reason", source="reason")
  RestaurantRejectedV1 toRejected(KitchenTicketEntity t, String reason);

  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="orderId", expression="java(t.getOrderId())")
  @Mapping(target="restaurantId", expression="java(t.getRestaurantId())")
  OrderReadyForPickupV1 toReady(KitchenTicketEntity t);
}
