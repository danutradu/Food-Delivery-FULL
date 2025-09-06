package com.example.food.order.app;

import com.example.food.order.domain.OrderEntity;
import com.example.food.order.domain.OutboxEventEntity;
import com.example.food.order.repo.OrderRepository;
import com.example.food.order.repo.OutboxRepository;
import com.example.food.order.web.dto.CreateOrderRequest;
import com.example.food.order.web.mapper.OrderEventMapper;
import com.example.food.order.web.mapper.OrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderAppService {

  private final OrderRepository orders;
  private final OutboxRepository outbox;
  private final OrderMapper orderMapper;
  private final OrderEventMapper eventMapper;
  private final ObjectMapper om;

  @Transactional
  public UUID createOrder(UUID customerUserId, CreateOrderRequest req) {
    OrderEntity order = orderMapper.toEntity(req, customerUserId);
    orders.save(order);

    stageOutbox("fd.order.created.v1", "fd.order.OrderCreatedV1",
        order.getId().toString(), toJson(eventMapper.toOrderCreated(order)));
    stageOutbox("fd.payment.requested.v1", "fd.payment.PaymentRequestedV1",
        order.getId().toString(), toJson(eventMapper.toPaymentRequested(order)));
    return order.getId();
  }

  private void stageOutbox(String topic, String eventType, String key, String payloadJson) {
    OutboxEventEntity e = new OutboxEventEntity();
    e.setId(UUID.randomUUID());
    e.setTopic(topic);
    e.setEventType(eventType);
    e.setKey(key);
    e.setPayloadJson(payloadJson);
    e.setCreatedAt(Instant.now());
    e.setPublished(false);
    outbox.save(e);
  }

  private String toJson(Object o) {
    try { return om.writeValueAsString(o); } catch (Exception e) { throw new RuntimeException(e); }
  }
}
