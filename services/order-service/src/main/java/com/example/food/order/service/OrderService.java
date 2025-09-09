package com.example.food.order.service;

import com.example.food.order.dto.CreateOrderRequest;
import com.example.food.order.dto.OrderResponse;
import com.example.food.order.mapper.OrderEventMapper;
import com.example.food.order.mapper.OrderMapper;
import com.example.food.order.model.OutboxEventEntity;
import com.example.food.order.repository.OrderRepository;
import com.example.food.order.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final OrderMapper orderMapper;
    private final OrderEventMapper eventMapper;
    private final ObjectMapper om;

    @Transactional
    public UUID createOrder(UUID customerUserId, CreateOrderRequest req) {
        log.info("Creating order customerUserId={} items={} currency={}", customerUserId, req.items().size(), req.currency());

        var order = orderMapper.toEntity(req, customerUserId);
        orders.save(order);

        stageOutbox("fd.order.created.v1", "fd.order.OrderCreatedV1",
                order.getId().toString(), toJson(eventMapper.toOrderCreated(order)));
        stageOutbox("fd.payment.requested.v1", "fd.payment.PaymentRequestedV1",
                order.getId().toString(), toJson(eventMapper.toPaymentRequested(order)));

        log.info("Order created successfully orderId={}", order.getId());
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
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OrderResponse createOrderWithResponse(UUID customerUserId, CreateOrderRequest req) {
        var orderId = createOrder(customerUserId, req);
        var items = req.items().stream().map(i -> new OrderResponse.Item(i.menuItemId(), i.name(), i.unitPriceCents(), i.quantity())).toList();
        var total = calculateTotal(req);
        return new OrderResponse(orderId, total, req.currency(), items);
    }

    public int calculateTotal(CreateOrderRequest req) {
        return req.items().stream().mapToInt(i -> i.unitPriceCents() * i.quantity()).sum();
    }
}
