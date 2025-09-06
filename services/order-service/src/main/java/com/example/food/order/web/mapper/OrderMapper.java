package com.example.food.order.web.mapper;

import com.example.food.order.domain.OrderEntity;
import com.example.food.order.domain.OrderItemEntity;
import com.example.food.order.web.dto.CreateOrderRequest;
import org.mapstruct.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class})
public interface OrderMapper {
  @Mapping(target="id", expression="java(UUID.randomUUID())")
  @Mapping(target="customerUserId", source="customerUserId")
  @Mapping(target="restaurantId", source="req.restaurantId")
  @Mapping(target="currency", source="req.currency")
  @Mapping(target="createdAt", expression="java(Instant.now())")
  @Mapping(target="items", source="req.items")
  @Mapping(target="totalCents", ignore = true)
  OrderEntity toEntity(CreateOrderRequest req, UUID customerUserId);

  @Mapping(target="id", expression="java(UUID.randomUUID())")
  @Mapping(target="order", ignore = true)
  OrderItemEntity toItem(CreateOrderRequest.Item src);

  List<OrderItemEntity> toItems(List<CreateOrderRequest.Item> src);

  @AfterMapping
  default void linkAndCompute(@MappingTarget OrderEntity order) {
    int total = 0;
    for (OrderItemEntity it : order.getItems()) {
      it.setOrder(order);
      total += it.getUnitPriceCents() * it.getQuantity();
    }
    order.setTotalCents(total);
  }
}
