package com.example.food.payment.mapping;

import com.example.food.payment.domain.PaymentEntity;
import fd.payment.PaymentAuthorizedV1;
import fd.payment.PaymentRequestedV1;
import org.mapstruct.*;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class})
public interface PaymentMapper {
  @Mapping(target="id", expression="java(UUID.randomUUID())")
  @Mapping(target="orderId", source="orderId")
  @Mapping(target="amountCents", source="amountCents")
  @Mapping(target="currency", source="currency")
  @Mapping(target="status", constant="AUTHORIZED")
  @Mapping(target="authorizationCode", ignore = true)
  @Mapping(target="createdAt", expression="java(Instant.now())")
  PaymentEntity fromRequested(PaymentRequestedV1 req);

  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="orderId", expression="java(entity.getOrderId())")
  @Mapping(target="amountCents", source="amountCents")
  @Mapping(target="currency", source="currency")
  @Mapping(target="authorizationCode", source="authorizationCode")
  PaymentAuthorizedV1 toAuthorized(PaymentEntity entity);
}
