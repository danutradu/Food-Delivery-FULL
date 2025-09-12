package com.example.food.order.service;

import com.example.food.order.model.OutboxEventEntity;
import com.example.food.order.repository.OutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fd.order.OrderCreatedV1;
import fd.order.OrderItem;
import fd.payment.PaymentRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, SpecificRecord> kafka;
    private final ObjectMapper om;

    @Value("${outbox.max-retries}")
    private int maxRetries;

    @Scheduled(fixedDelay = 1000L)
    @Transactional
    public void publishBatch() {

        var batch = outbox.findTop50ByPublishedFalseOrderByCreatedAtAsc();
        if (batch.isEmpty()) {
            return;
        }

        log.debug("Publishing {} outbox events", batch.size());

        for (var event : batch) {
            try {
                var avro = toAvro(event);
                ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>(event.getTopic(), event.getKey(), avro);
                record.headers().add("eventType", event.getEventType().getBytes());
                record.headers().add("eventId", event.getId().toString().getBytes());

                kafka.send(record).get(); // Keep synchronous for outbox reliability
                event.setPublished(true);
                log.debug("Successfully published event id={} topic={}", event.getId(), event.getTopic());
            } catch (Exception ex) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastRetryAt(Instant.now());

                if (event.getRetryCount() >= maxRetries) {
                    log.error("Max retries exceeded for outbox event id={} topic={}, giving up", event.getId(), event.getTopic(), ex);
                    event.setPublished(true); // Mark as published to stop retrying (could move to DLQ table instead)
                } else {
                    log.warn("Failed to publish outbox event id={} topic={}, retry {}/{}", event.getId(), event.getTopic(), event.getRetryCount(), maxRetries, ex);
                }
            }
        }
    }

    private static Instant readInstant(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v.isNumber()) return Instant.ofEpochMilli(v.asLong());
        return Instant.parse(v.asText());
    }

    private SpecificRecord toAvro(OutboxEventEntity e) throws Exception {
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
