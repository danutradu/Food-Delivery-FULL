package com.example.food.ops.service;

import com.example.food.ops.dto.StatusUpdateRequest;
import com.example.food.ops.exception.KitchenTicketNotFoundException;
import com.example.food.ops.mapper.OpsMapper;
import com.example.food.ops.model.KitchenTicketEntity;
import com.example.food.ops.model.KitchenTicketStatus;
import com.example.food.ops.repository.KitchenTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpsService {
    private final KitchenTicketRepository ticketRepository;
    private final KafkaTemplate<String, SpecificRecord> kafka;
    private final OpsMapper mapper;

    public void processAcceptanceRequest(UUID orderId, UUID restaurantId) {
        log.info("Process acceptance request orderId={} restaurantId={}", orderId, restaurantId);

        var ticket = ticketRepository.findByOrderId(orderId).orElseGet(() -> {
            var newTicket = new KitchenTicketEntity();
            newTicket.setId(UUID.randomUUID());
            newTicket.setOrderId(orderId);
            newTicket.setRestaurantId(restaurantId);
            newTicket.setStatus(KitchenTicketStatus.PENDING);
            newTicket.setReceivedAt(Instant.now());
            return newTicket;
        });
        ticketRepository.save(ticket);
    }

    public KitchenTicketEntity updateStatus(UUID orderId, StatusUpdateRequest request) {
        log.info("Updating status orderId={} status={}", orderId, request.status());

        var ticket = ticketRepository.findByOrderId(orderId)
                .orElseThrow(() -> new KitchenTicketNotFoundException(orderId.toString()));

        switch (request.status()) {
            case ACCEPTED -> {
                ticket.setStatus(KitchenTicketStatus.ACCEPTED);
                ticket.setAcceptedAt(Instant.now());
                if (request.etaMinutes() != null) {
                    ticket.setEstimatedPrepTimeMinutes(request.etaMinutes());
                }
                publishAcceptedEvent(ticket, request.etaMinutes() != null ? request.etaMinutes() : 15);
            }
            case REJECTED -> {
                ticket.setStatus(KitchenTicketStatus.REJECTED);
                publishRejectedEvent(ticket, request.reason() != null ? request.reason() : "Out of stock");
            }
            case IN_PROGRESS -> {
                ticket.setStatus(KitchenTicketStatus.IN_PROGRESS);
                ticket.setStartedAt(Instant.now());
            }
            case READY -> {
                ticket.setStatus(KitchenTicketStatus.READY);
                ticket.setReadyAt(Instant.now());
                publishReadyEvent(ticket);
            }
            default -> throw new IllegalArgumentException("Invalid status transition: " + request.status());
        }

        return ticketRepository.save(ticket);
    }

    private void publishAcceptedEvent(KitchenTicketEntity ticket, int etaMinutes) {
        var event = mapper.toAccepted(ticket, etaMinutes);
        ProducerRecord<String, SpecificRecord> rec = new ProducerRecord<>("fd.restaurant.accepted.v1", ticket.getOrderId().toString(), event);
        rec.headers().add("eventType", "fd.restaurant.RestaurantAcceptedV1".getBytes());
        rec.headers().add("eventId", event.getOrderId().toString().getBytes());

        kafka.send(rec).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish restaurant accepted event orderId={}", ticket.getOrderId(), ex);
            } else {
                log.info("Published restaurant accepted event orderId={}", ticket.getOrderId());
            }
        });
    }

    private void publishRejectedEvent(KitchenTicketEntity ticket, String reason) {
        var event = mapper.toRejected(ticket, reason);
        ProducerRecord<String, SpecificRecord> rec = new ProducerRecord<>("fd.restaurant.rejected.v1", ticket.getOrderId().toString(), event);
        rec.headers().add("eventType", "fd.restaurant.RestaurantRejectedV1".getBytes());
        rec.headers().add("eventId", event.getOrderId().toString().getBytes());

        kafka.send(rec).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish restaurant rejected event orderId={}", ticket.getOrderId(), ex);
            } else {
                log.info("Published restaurant rejected event orderId={}", ticket.getOrderId());
            }
        });
    }

    private void publishReadyEvent(KitchenTicketEntity ticket) {
        var event = mapper.toReady(ticket);
        ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>("fd.restaurant.order-ready.v1", ticket.getOrderId().toString(), event);
        record.headers().add("eventType", "fd.restaurant.OrderReadyForPickupV1".getBytes());
        record.headers().add("eventId", event.getOrderId().toString().getBytes());

        kafka.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish order ready event orderId={}", ticket.getOrderId(), ex);
            } else {
                log.info("Published order ready event orderId={}", ticket.getOrderId());
            }
        });
    }

    public void startCooking(UUID orderId) {
        log.info("Starting cooking orderId={}", orderId);

        var ticket = ticketRepository.findByOrderId(orderId)
                .orElseThrow(() -> new KitchenTicketNotFoundException(orderId.toString()));

        ticket.setStatus(KitchenTicketStatus.IN_PROGRESS);
        ticket.setStartedAt(Instant.now());

        ticketRepository.save(ticket);

        log.info("Order cooking started orderId={}", orderId);
    }

    public List<KitchenTicketEntity> getTickets(KitchenTicketStatus status) {
        if (status != null) {
            return ticketRepository.findByStatus(status);
        }
        return ticketRepository.findAll();
    }

    public KitchenTicketEntity getTicket(UUID orderId) {
        return ticketRepository.findByOrderId(orderId)
                .orElseThrow(() -> new KitchenTicketNotFoundException(orderId.toString()));
    }
}
