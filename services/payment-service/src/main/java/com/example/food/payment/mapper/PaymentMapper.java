package com.example.food.payment.mapper;

import com.example.food.payment.model.PaymentEntity;
import fd.payment.PaymentAuthorizedV1;
import fd.payment.PaymentFailedV1;
import fd.payment.PaymentRequestedV1;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class}, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentMapper {
    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "amountCents", source = "amountCents")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "authorizationCode", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    PaymentEntity fromRequested(PaymentRequestedV1 req);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "occurredAt", expression = "java(Instant.now())")
    @Mapping(target = "orderId", expression = "java(entity.getOrderId())")
    @Mapping(target = "amountCents", source = "amountCents")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "authorizationCode", source = "authorizationCode")
    PaymentAuthorizedV1 toAuthorized(PaymentEntity entity);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "occurredAt", expression = "java(Instant.now())")
    @Mapping(target = "orderId", expression = "java(entity.getOrderId())")
    @Mapping(target = "amountCents", source = "amountCents")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "failureReason", source = "failureReason")
    PaymentFailedV1 toFailed(PaymentEntity entity);
}
