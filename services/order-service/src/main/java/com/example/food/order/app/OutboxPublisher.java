package com.example.food.order.app;

import com.example.food.order.domain.OutboxEventEntity;
import com.example.food.order.repo.OutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fd.order.OrderCreatedV1;
import fd.order.OrderItem;
import fd.payment.PaymentRequestedV1;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

  private final OutboxRepository outbox;
  private final KafkaTemplate<String, Object> kafka;
  private final ObjectMapper om;

  @Scheduled(fixedDelay = 1000L)
  @Transactional
  public void publishBatch() throws Exception {
    List<OutboxEventEntity> batch = outbox.findTop50ByPublishedFalseOrderByCreatedAtAsc();
    for (OutboxEventEntity e : batch) {
      Object avro = toAvro(e);
      ProducerRecord<String,Object> record = new ProducerRecord<>(e.getTopic(), e.getKey(), avro);
      record.headers().add("eventType", e.getEventType().getBytes());
      kafka.send(record).get();
      e.setPublished(true);
    }
  }

  private static Instant readInstant(JsonNode n, String field) {
    JsonNode v = n.get(field);
    if (v.isNumber()) return Instant.ofEpochMilli(v.asLong());
    return Instant.parse(v.asText());
  }

  private Object toAvro(OutboxEventEntity e) throws Exception {
    JsonNode n = om.readTree(e.getPayloadJson());
    String t = e.getEventType();
    if ("fd.order.OrderCreatedV1".equals(t)) {
      List<OrderItem> items = new ArrayList<>();
      for (JsonNode it : n.get("items")) {
        items.add(new OrderItem(
            java.util.UUID.fromString(it.get("menuItemId").asText()),
            it.get("name").asText(),
            it.get("unitPriceCents").asInt(),
            it.get("quantity").asInt()
        ));
      }
      return new OrderCreatedV1(
          java.util.UUID.fromString(n.get("eventId").asText()),
          readInstant(n, "occurredAt"),
          java.util.UUID.fromString(n.get("orderId").asText()),
          java.util.UUID.fromString(n.get("customerUserId").asText()),
          java.util.UUID.fromString(n.get("restaurantId").asText()),
          n.get("totalCents").asInt(),
          n.get("currency").asText(),
          items
      );
    } else if ("fd.payment.PaymentRequestedV1".equals(t)) {
      return new PaymentRequestedV1(
          java.util.UUID.fromString(n.get("eventId").asText()),
          readInstant(n, "occurredAt"),
          java.util.UUID.fromString(n.get("orderId").asText()),
          n.get("amountCents").asInt(),
          n.get("currency").asText()
      );
    } else {
      throw new IllegalArgumentException("Unknown eventType " + t);
    }
  }
}
