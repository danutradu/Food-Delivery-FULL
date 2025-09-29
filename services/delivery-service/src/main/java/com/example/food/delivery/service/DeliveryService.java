package com.example.food.delivery.service;

import com.example.food.common.outbox.OutboxService;
import com.example.food.delivery.config.KafkaTopics;
import com.example.food.delivery.exception.AssignmentNotFoundException;
import com.example.food.delivery.model.AssignmentStatus;
import com.example.food.delivery.repository.AssignmentRepository;
import com.example.food.delivery.util.DeliveryFactory;
import fd.delivery.DeliveryRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {
    private final AssignmentRepository assignmentRepository;
    private final OutboxService outboxService;
    private final KafkaTopics topics;

    @Transactional
    public void markAsPickedUp(UUID assignmentId) {
        log.info("Marking assignment as picked up assignmentId={}", assignmentId);

        var assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId.toString()));

        if (assignment.getStatus() != AssignmentStatus.ASSIGNED) {
            throw new IllegalStateException("Cannot pick up assignment in status: " + assignment.getStatus());
        }

        assignment.setStatus(AssignmentStatus.PICKED_UP);
        assignmentRepository.save(assignment);

        var event = DeliveryFactory.createOrderPickedUp(assignment);
        outboxService.publish(topics.getOrderPickedUp(), assignment.getOrderId().toString(), event);

        log.info("Order marked as picked up orderId={}", assignment.getOrderId());
    }

    @Transactional
    public void markAsDelivered(UUID assignmentId) {
        log.info("Marking assignment as delivered assignmentId={}", assignmentId);

        var assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId.toString()));

        if (assignment.getStatus() != AssignmentStatus.PICKED_UP) {
            throw new IllegalStateException("Cannot deliver assignment that hasn't been picked up. Current status: " + assignment.getStatus());
        }

        assignment.setStatus(AssignmentStatus.DELIVERED);
        assignmentRepository.save(assignment);

        var event = DeliveryFactory.createOrderDelivered(assignment);
        outboxService.publish(topics.getOrderDelivered(), assignment.getOrderId().toString(), event);

        log.info("Order marked as delivered orderId={}", assignment.getOrderId());
    }

    @Transactional
    public void processDeliveryRequest(DeliveryRequestedV1 event) {
        log.info("Processing delivery request orderId={} restaurantId={}", event.getOrderId(), event.getRestaurantId());

        var assignment = assignmentRepository.findByOrderId(event.getOrderId()) // TODO: verify why orElseGet instead of NEW? i know it's for idempotency but is the only way?
                .orElseGet(() -> DeliveryFactory.createAssignment(event));
        assignmentRepository.save(assignment);

        var assignedEvent = DeliveryFactory.createCourierAssigned(assignment);
        outboxService.publish(topics.getCourierAssigned(), event.getOrderId().toString(), assignedEvent);

        log.info("Courier assigned for delivery orderId={}", assignment.getOrderId());
    }

    @Transactional
    public void updateAssignmentStatus(UUID assignmentId, AssignmentStatus newStatus) {
        log.info("Updating assignment status assignmentId={} newStatus={}", assignmentId, newStatus);

        switch (newStatus) {
            case PICKED_UP -> markAsPickedUp(assignmentId);
            case DELIVERED -> markAsDelivered(assignmentId);
            default -> throw new IllegalArgumentException("Invalid status transition: " + newStatus);
        }
    }

    public void cancelDelivery(UUID orderId, String reason) {
        log.info("Cancelling delivery orderId={} reason={}", orderId, reason);

        var assignment = assignmentRepository.findByOrderId(orderId).orElse(null);
        if (assignment == null) {
            log.warn("No delivery assignment found for cancelled order orderId={}", orderId);
            return;
        }

        if (assignment.getStatus() != AssignmentStatus.DELIVERED) {
            assignment.setStatus(AssignmentStatus.CANCELLED);
            assignmentRepository.save(assignment);
            log.info("Delivery assignment cancelled orderId={}", orderId);
        } else {
            log.info("Delivery already completed, not cancelling orderId={}", orderId);
        }
    }
}
