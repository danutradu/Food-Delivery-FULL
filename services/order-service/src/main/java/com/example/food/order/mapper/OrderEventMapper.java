package com.example.food.order.mapper;

import com.example.food.order.model.OrderEntity;
import com.example.food.order.model.OrderItemEntity;
import fd.order.OrderCreatedV1;
import fd.order.OrderItem;
import fd.payment.PaymentRequestedV1;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class}, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderEventMapper {
  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="orderId", expression="java(order.getId())")
  @Mapping(target="customerUserId", expression="java(order.getCustomerUserId())")
  @Mapping(target="restaurantId", expression="java(order.getRestaurantId())")
  @Mapping(target="items", expression="java(toAvroItems(order.getItems()))")
  OrderCreatedV1 toOrderCreated(OrderEntity order);

  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="orderId", expression="java(order.getId())")
  @Mapping(target="amountCents", source="totalCents")
  @Mapping(target="currency", source="currency")
  PaymentRequestedV1 toPaymentRequested(OrderEntity order);

  default List<OrderItem> toAvroItems(List<OrderItemEntity> items) {
    return items.stream().map(i -> new OrderItem(
        i.getMenuItemId(), i.getName(), i.getUnitPriceCents(), i.getQuantity()
    )).toList();
  }
}
