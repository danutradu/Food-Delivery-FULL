package com.example.food.ops.service;

import com.example.food.ops.exception.KitchenTicketNotFoundException;
import com.example.food.ops.mapper.OpsMapper;
import com.example.food.ops.model.KitchenTicketEntity;
import com.example.food.ops.repository.KitchenTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpsService {
    private final KitchenTicketRepository tickets;
    private final KafkaTemplate<String, SpecificRecord> kafka;
    private final OpsMapper mapper;

    public void acceptOrder(UUID orderId, int etaMinutes) {
        log.info("Accepting order orderId={} etaMinutes={}", orderId, etaMinutes);

        var ticket = tickets.findByOrderId(orderId)
                .orElseThrow(() -> new KitchenTicketNotFoundException(orderId.toString()));

        ticket.setStatus("ACCEPTED");
        tickets.save(ticket);

        var event = mapper.toAccepted(ticket, etaMinutes);
        ProducerRecord<String, SpecificRecord> rec = new ProducerRecord<>("fd.restaurant.accepted.v1", ticket.getOrderId().toString(), event);
        rec.headers().add("eventType", "fd.restaurant.RestaurantAcceptedV1".getBytes());
        rec.headers().add("eventId", event.getOrderId().toString().getBytes());

        kafka.send(rec).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish restaurant accepted event orderId={}", orderId, ex);
            } else {
                log.info("Published restaurant accepted event orderId={}", orderId);
            }
        });
    }

    public void rejectOrder(UUID orderId, String reason) {
        log.info("Rejection order orderId={} reason={}", orderId, reason);

        var ticket = tickets.findByOrderId(orderId)
                .orElseThrow(() -> new KitchenTicketNotFoundException(orderId.toString()));

        ticket.setStatus("REJECTED");
        tickets.save(ticket);

        var event = mapper.toRejected(ticket, reason);
        ProducerRecord<String, SpecificRecord> rec = new ProducerRecord<>("fd.restaurant.rejected.v1", ticket.getOrderId().toString(), event);
        rec.headers().add("eventType", "fd.restaurant.RestaurantRejectedV1".getBytes());
        rec.headers().add("eventId", event.getOrderId().toString().getBytes());

        kafka.send(rec).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish restaurant rejected event orderId={}", orderId, ex);
            } else {
                log.info("Published restaurant rejected event orderId={}", orderId);
            }
        });
    }

    public void markOrderReady(UUID orderId) {
        log.info("Marking order ready orderId={}", orderId);

        var ticket = tickets.findByOrderId(orderId)
                .orElseThrow(() -> new KitchenTicketNotFoundException(orderId.toString()));

        ticket.setStatus("READY");
        tickets.save(ticket);

        var event = mapper.toReady(ticket);
        ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>("fd.restaurant.order-ready.v1", ticket.getOrderId().toString(), event);
        record.headers().add("eventType", "fd.restaurant.OrderReadyForPickupV1".getBytes());
        record.headers().add("eventId", event.getOrderId().toString().getBytes());

        kafka.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish order ready event orderId={}", orderId, ex);
            } else {
                log.info("Published order ready event orderId={}", orderId);
            }
        });
    }

    public void processAcceptanceRequest(UUID orderId, UUID restaurantId) {
        log.info("Process acceptance request orderId={} restaurantId={}", orderId, restaurantId);

        var ticket = tickets.findByOrderId(orderId).orElseGet(() -> {
            var newTicket = new KitchenTicketEntity();
            newTicket.setId(UUID.randomUUID());
            newTicket.setOrderId(orderId);
            newTicket.setRestaurantId(restaurantId);
            newTicket.setStatus("PENDING");
            return newTicket;
        });
        tickets.save(ticket);
    }
}
