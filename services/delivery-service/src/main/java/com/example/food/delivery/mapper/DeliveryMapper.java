package com.example.food.delivery.mapper;

import com.example.food.delivery.model.AssignmentEntity;
import fd.delivery.CourierAssignedV1;
import fd.delivery.DeliveryRequestedV1;
import fd.delivery.OrderDeliveredV1;
import fd.delivery.OrderPickedUpV1;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class})
public interface DeliveryMapper {
  @Mapping(target="id", expression="java(UUID.randomUUID())")
  @Mapping(target="orderId", source="orderId")
  @Mapping(target="courierId", expression="java(UUID.randomUUID())")
  @Mapping(target="status", constant="ASSIGNED")
  @Mapping(target="assignedAt", expression="java(Instant.now())")
  AssignmentEntity fromRequested(DeliveryRequestedV1 req);

  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="assignmentId", expression="java(a.getId())")
  @Mapping(target="orderId", expression="java(a.getOrderId())")
  @Mapping(target="courierId", expression="java(a.getCourierId())")
  CourierAssignedV1 toAssigned(AssignmentEntity a);

  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="orderId", expression="java(a.getOrderId())")
  @Mapping(target="courierId", expression="java(a.getCourierId())")
  OrderPickedUpV1 toPickedUp(AssignmentEntity a);

  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="orderId", expression="java(a.getOrderId())")
  @Mapping(target="courierId", expression="java(a.getCourierId())")
  OrderDeliveredV1 toDelivered(AssignmentEntity a);
}
