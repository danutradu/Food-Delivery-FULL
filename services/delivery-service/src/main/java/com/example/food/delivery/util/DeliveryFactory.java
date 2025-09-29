package com.example.food.delivery.util;

import com.example.food.delivery.model.AssignmentEntity;
import com.example.food.delivery.model.AssignmentStatus;
import fd.delivery.CourierAssignedV1;
import fd.delivery.DeliveryRequestedV1;
import fd.delivery.OrderDeliveredV1;
import fd.delivery.OrderPickedUpV1;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class DeliveryFactory {

    public AssignmentEntity createAssignment(DeliveryRequestedV1 req) {
        var assignment = new AssignmentEntity();
        assignment.setId(UUID.randomUUID());
        assignment.setOrderId(req.getOrderId());
        assignment.setCourierId(UUID.randomUUID()); // Auto-assign random courier
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(Instant.now());
        return assignment;
    }

    public CourierAssignedV1 createCourierAssigned(AssignmentEntity assignment) {
        return new CourierAssignedV1(
                UUID.randomUUID(),
                Instant.now(),
                assignment.getId(),
                assignment.getOrderId(),
                assignment.getCourierId()
        );
    }

    public OrderPickedUpV1 createOrderPickedUp(AssignmentEntity assignment) {
        return new OrderPickedUpV1(
                UUID.randomUUID(),
                Instant.now(),
                assignment.getOrderId(),
                assignment.getCourierId()
        );
    }

    public OrderDeliveredV1 createOrderDelivered(AssignmentEntity assignment) {
        return new OrderDeliveredV1(
                UUID.randomUUID(),
                Instant.now(),
                assignment.getOrderId(),
                assignment.getCourierId()
        );
    }
}
