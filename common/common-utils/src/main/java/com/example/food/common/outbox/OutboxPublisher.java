package com.example.food.common.outbox;

import fd.cart.CartCheckedOutV1;
import fd.catalog.MenuItemUpdatedV1;
import fd.catalog.RestaurantCreatedV1;
import fd.delivery.CourierAssignedV1;
import fd.delivery.DeliveryRequestedV1;
import fd.delivery.OrderDeliveredV1;
import fd.delivery.OrderPickedUpV1;
import fd.order.OrderCancelledV1;
import fd.order.OrderCreatedV1;
import fd.payment.*;
import fd.restaurant.OrderReadyForPickupV1;
import fd.restaurant.RestaurantAcceptanceRequestedV1;
import fd.restaurant.RestaurantAcceptedV1;
import fd.restaurant.RestaurantRejectedV1;
import fd.user.UserRegisteredV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;

    @Value("${outbox.max-retries:5}")
    private int maxRetries;

    @Value("${outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelay = 1000L)
    @Transactional
    public void publishBatch() {
        var pageable = PageRequest.of(0, batchSize, Sort.by("createdAt"));
        var batch = outboxRepository.findByStatus(OutboxStatus.PENDING, pageable);
        if (batch.isEmpty()) {
            return;
        }

        log.debug("Publishing {} outbox events", batch.size());

        for (var event : batch) {
            try {
                var avro = deserializeEvent(event);
                ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>(event.getTopic(), event.getKey(), avro);
                record.headers().add("eventType", event.getEventType().getBytes());
                record.headers().add("eventId", event.getId().toString().getBytes());

                kafkaTemplate.send(record).whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish outbox event id={}, will retry", event.getId(), ex);
                        event.setRetryCount(event.getRetryCount() + 1);
                        event.setLastRetryAt(Instant.now());
                        event.setLastError(ex.getMessage());

                        if (event.getRetryCount() >= maxRetries) {
                            event.setStatus(OutboxStatus.PARKED);
                            log.error("Max retries exceeded for outbox event id={} topic={}, PARKED for manual intervention",
                                    event.getId(), event.getTopic(), ex);
                        }
                        outboxRepository.save(event);
                    } else {
                        event.setPublished(true);
                        event.setStatus(OutboxStatus.PUBLISHED);
                        event.setPublishedAt(Instant.now());
                        event.setLastError(null);
                        outboxRepository.save(event);
                        log.debug("Successfully published event id={} topic={}", event.getId(), event.getTopic());
                    }
                });
            } catch (Exception ex) {
                log.error("Failed to process outbox event id={}, will retry", event.getId(), ex);
            }
        }
    }

    private SpecificRecord deserializeEvent(OutboxEventEntity event) throws Exception {
        return switch (event.getEventType()) {
            case "CartCheckedOutV1" -> deserialize(CartCheckedOutV1.class, event.getPayloadJson());
            case "MenuItemUpdatedV1" -> deserialize(MenuItemUpdatedV1.class, event.getPayloadJson());
            case "RestaurantCreatedV1" -> deserialize(RestaurantCreatedV1.class, event.getPayloadJson());
            case "CourierAssignedV1" -> deserialize(CourierAssignedV1.class, event.getPayloadJson());
            case "OrderDeliveredV1" -> deserialize(OrderDeliveredV1.class, event.getPayloadJson());
            case "OrderPickedUpV1" -> deserialize(OrderPickedUpV1.class, event.getPayloadJson());
            case "OrderCreatedV1" -> deserialize(OrderCreatedV1.class, event.getPayloadJson());
            case "PaymentAuthorizedV1" -> deserialize(PaymentAuthorizedV1.class, event.getPayloadJson());
            case "PaymentFailedV1" -> deserialize(PaymentFailedV1.class, event.getPayloadJson());
            case "RefundCompletedV1" -> deserialize(RefundCompletedV1.class, event.getPayloadJson());
            case "OrderReadyForPickupV1" -> deserialize(OrderReadyForPickupV1.class, event.getPayloadJson());
            case "RestaurantAcceptedV1" -> deserialize(RestaurantAcceptedV1.class, event.getPayloadJson());
            case "RestaurantRejectedV1" -> deserialize(RestaurantRejectedV1.class, event.getPayloadJson());
            case "UserRegisteredV1" -> deserialize(UserRegisteredV1.class, event.getPayloadJson());
            case "PaymentRequestedV1" -> deserialize(PaymentRequestedV1.class, event.getPayloadJson());
            case "RefundRequestedV1" -> deserialize(RefundRequestedV1.class, event.getPayloadJson());
            case "FeeRequestedV1" -> deserialize(FeeRequestedV1.class, event.getPayloadJson());
            case "OrderCancelledV1" -> deserialize(OrderCancelledV1.class, event.getPayloadJson());
            case "RestaurantAcceptanceRequestedV1" ->
                    deserialize(RestaurantAcceptanceRequestedV1.class, event.getPayloadJson());
            case "DeliveryRequestedV1" -> deserialize(DeliveryRequestedV1.class, event.getPayloadJson());
            default -> throw new IllegalArgumentException("Unknown eventType " + event.getEventType());
        };
    }

    private <T extends SpecificRecord> T deserialize(Class<T> clazz, String payloadJson) throws Exception {
        var reader = new SpecificDatumReader<T>(clazz);
        var instance = clazz.getDeclaredConstructor().newInstance();
        var decoder = DecoderFactory.get().jsonDecoder(instance.getSchema(), payloadJson);
        return reader.read(null, decoder);
    }
}
