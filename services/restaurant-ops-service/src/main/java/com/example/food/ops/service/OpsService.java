package com.example.food.ops.service;

import com.example.food.ops.exception.KitchenTicketNotFoundException;
import com.example.food.ops.mapper.OpsMapper;
import com.example.food.ops.model.KitchenTicketEntity;
import com.example.food.ops.repository.KitchenTicketRepository;
import fd.restaurant.OrderReadyForPickupV1;
import fd.restaurant.RestaurantAcceptedV1;
import fd.restaurant.RestaurantRejectedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpsService {
    private final KitchenTicketRepository tickets;
    private final KafkaTemplate<String, Object> kafka;
    private final OpsMapper mapper;

    @Transactional
    public void acceptOrder(UUID orderId, int etaMinutes) {
        log.info("Accepting order orderId={} etaMinutes={}", orderId, etaMinutes);

        var ticket = tickets.findByOrderId(orderId)
                .orElseThrow(() -> new KitchenTicketNotFoundException("Kitchen ticket not found for order: " + orderId));

        ticket.setStatus("ACCEPTED");
        tickets.save(ticket);

        var event = mapper.toAccepted(ticket, etaMinutes);
        ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.restaurant.accepted.v1", ticket.getOrderId().toString(), event);
        rec.headers().add("eventType","fd.restaurant.RestaurantAcceptedV1".getBytes());
        kafka.send(rec);
    }

    @Transactional
    public void rejectOrder(UUID orderId, String reason) {
        log.info("Rejection order orderId={} reason={}", orderId, reason);

        var ticket = tickets.findByOrderId(orderId)
                .orElseThrow(() -> new KitchenTicketNotFoundException("Kitchen ticket not found for order: " + orderId));

        ticket.setStatus("REJECTED");
        tickets.save(ticket);

        var event = mapper.toRejected(ticket, reason);
        ProducerRecord<String, Object> rec = new ProducerRecord<>("fd.restaurant.rejected.v1", ticket.getOrderId().toString(), event);
        rec.headers().add("eventType", "fd.restaurant.RestaurantRejectedV1".getBytes());
        kafka.send(rec);
    }

    @Transactional
    public void markOrderReady(UUID orderId) {
        log.info("Marking order ready orderId={}", orderId);

        var ticket = tickets.findByOrderId(orderId)
                .orElseThrow(() -> new KitchenTicketNotFoundException("Kitchen ticket not found for order: " + orderId));

        ticket.setStatus("READY");
        tickets.save(ticket);

        var event = mapper.toReady(ticket);
        ProducerRecord<String, Object> rec = new ProducerRecord<>("fd.restaurant.order-ready.v1", ticket.getOrderId().toString(), event);
        rec.headers().add("eventType", "fd.restaurant.OrderReadyForPickupV1".getBytes());
        kafka.send(rec);
    }

    @Transactional
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
