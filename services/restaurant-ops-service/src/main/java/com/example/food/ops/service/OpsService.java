package com.example.food.ops.service;

import com.example.food.common.outbox.OutboxService;
import com.example.food.ops.config.KafkaTopics;
import com.example.food.ops.dto.StatusUpdateRequest;
import com.example.food.ops.exception.KitchenTicketNotFoundException;
import com.example.food.ops.model.KitchenTicketEntity;
import com.example.food.ops.model.KitchenTicketStatus;
import com.example.food.ops.repository.KitchenTicketRepository;
import com.example.food.ops.util.OpsEventFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpsService {
    private final KitchenTicketRepository ticketRepository;
    private final OutboxService outboxService;
    private final KafkaTopics topics;

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

    @Transactional
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
                var acceptedEvent = OpsEventFactory.createRestaurantAccepted(ticket, request.etaMinutes() != null ? request.etaMinutes() : 15);
                outboxService.publish(topics.getRestaurantAccepted(), acceptedEvent.getOrderId().toString(), acceptedEvent);
            }
            case REJECTED -> {
                ticket.setStatus(KitchenTicketStatus.REJECTED);
                var rejectedEvent = OpsEventFactory.createRestaurantRejected(ticket, request.reason() != null ? request.reason() : "Out of stock");
                outboxService.publish(topics.getRestaurantRejected(), rejectedEvent.getOrderId().toString(), rejectedEvent);
            }
            case IN_PROGRESS -> {
                ticket.setStatus(KitchenTicketStatus.IN_PROGRESS);
                ticket.setStartedAt(Instant.now());
            }
            case READY -> {
                ticket.setStatus(KitchenTicketStatus.READY);
                ticket.setReadyAt(Instant.now());
                var readyEvent = OpsEventFactory.createOrderReady(ticket);
                outboxService.publish(topics.getOrderReady(), readyEvent.getOrderId().toString(), readyEvent);
            }
            default -> throw new IllegalArgumentException("Invalid status transition: " + request.status());
        }

        return ticketRepository.save(ticket);
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

    public void cancelKitchenTicket(UUID orderId, String reason) {
        log.info("Cancelling kitchen ticket orderId={} reason={}", orderId, reason);

        var ticket = ticketRepository.findByOrderId(orderId).orElseThrow(null);
        if (ticket == null) {
            log.warn("No kitchen ticket found for cancelled order orderId={}", orderId);
            return;
        }

        // Only cancel if not already completed
        if (ticket.getStatus() != KitchenTicketStatus.READY) {
            ticket.setStatus(KitchenTicketStatus.CANCELLED);
            ticketRepository.save(ticket);
            log.info("Kitchen ticket cancelled orderId={}", orderId);
        } else {
            log.info("Kitchen ticket already ready, not cancelling orderId={}", orderId);
        }
    }
}
