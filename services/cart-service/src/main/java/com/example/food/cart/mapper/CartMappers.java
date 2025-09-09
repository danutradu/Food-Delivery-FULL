package com.example.food.cart.mapper;

import com.example.food.cart.model.CartEntity;
import com.example.food.cart.model.CartItemEntity;
import com.example.food.cart.dto.AddItemRequest;
import fd.cart.CartCheckedOutV1;
import fd.cart.CartItem;
import org.mapstruct.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class})
public interface CartMappers {
  @Mapping(target="id", expression="java(UUID.randomUUID())")
  @Mapping(target="cart", ignore = true)
  CartItemEntity toItem(AddItemRequest req);

  default List<CartItem> toAvroItems(List<CartItemEntity> items) {
    return items.stream().map(i -> new CartItem(
        i.getMenuItemId(), i.getName(), i.getUnitPriceCents(), i.getQuantity()
    )).toList();
  }

  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="cartId", expression="java(cart.getId())")
  @Mapping(target="customerUserId", expression="java(cart.getCustomerUserId())")
  @Mapping(target="restaurantId", expression="java(cart.getRestaurantId())")
  @Mapping(target="currency", source="currency")
  @Mapping(target="totalCents", expression="java(cart.getItems().stream().mapToInt(i->i.getUnitPriceCents()*i.getQuantity()).sum())")
  @Mapping(target="items", expression="java(toAvroItems(cart.getItems()))")
  CartCheckedOutV1 toCheckedOut(CartEntity cart);
}
