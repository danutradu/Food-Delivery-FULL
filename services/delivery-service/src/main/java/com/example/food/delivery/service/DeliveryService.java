package com.example.food.delivery.service;

import com.example.food.delivery.exception.AssignmentNotFoundException;
import com.example.food.delivery.mapper.DeliveryMapper;
import com.example.food.delivery.repository.AssignmentRepository;
import fd.delivery.DeliveryRequestedV1;
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
public class DeliveryService {
    private final AssignmentRepository assignments;
    private final KafkaTemplate<String, SpecificRecord> kafka;
    private final DeliveryMapper mapper;

    public void markAsPickedUp(UUID assignmentId) {
        log.info("Marking assignment as picked up assignmentId={}", assignmentId);

        var assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found: " + assignmentId));

        assignment.setStatus("PICKED_UP");
        assignments.save(assignment);

        var event = mapper.toPickedUp(assignment);
        ProducerRecord<String, SpecificRecord> rec = new ProducerRecord<>("fd.delivery.picked-up.v1", assignment.getOrderId().toString(), event);
        rec.headers().add("eventType", "fd.delivery.OrderPickedUpV1".getBytes());
        rec.headers().add("eventId", event.getEventId().toString().getBytes());

        kafka.send(rec);
        log.info("Published order picked up event orderId={}", assignment.getOrderId());
    }

    public void markAsDelivered(UUID assignmentId) {
        log.info("Marking assignment as delivered assignmentId={}", assignmentId);

        var assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found: " + assignmentId));

        assignment.setStatus("DELIVERED");
        assignments.save(assignment);

        var event = mapper.toDelivered(assignment);
        ProducerRecord<String, SpecificRecord> rec = new ProducerRecord<>("fd.delivery.delivered.v1", assignment.getOrderId().toString(), event);
        rec.headers().add("eventType", "fd.delivery.OrderDeliveredV1".getBytes());
        rec.headers().add("eventId", event.getEventId().toString().getBytes());

        kafka.send(rec);
        log.info("Published order delivered event orderId={}", assignment.getOrderId());
    }

    public void processDeliveryRequest(DeliveryRequestedV1 event) {
        log.info("Processing delivery request orderId={} restaurantId={}", event.getOrderId(), event.getRestaurantId());

        var assignment = assignments.findByOrderId(event.getOrderId())
                .orElseGet(() -> mapper.fromRequested(event));
        assignments.save(assignment);

        var assignedEvent = mapper.toAssigned(assignment);
        ProducerRecord<String, SpecificRecord> rec = new ProducerRecord<>("fd.delivery.courier-assigned.v1", event.getOrderId().toString(), assignedEvent);
        rec.headers().add("eventType", "fd.delivery.CourierAssignedV1".getBytes());
        rec.headers().add("eventId", event.getEventId().toString().getBytes());

        kafka.send(rec);
        log.info("Published courier assigned event orderId={}", assignment.getOrderId());
    }
}
