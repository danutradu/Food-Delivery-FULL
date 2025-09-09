package com.example.food.delivery.controller;

import com.example.food.delivery.model.AssignmentEntity;
import com.example.food.delivery.mapper.DeliveryMapper;
import com.example.food.delivery.repository.AssignmentRepository;
import com.example.food.delivery.service.DeliveryService;
import fd.delivery.OrderDeliveredV1;
import fd.delivery.OrderPickedUpV1;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/delivery/tasks")
@Slf4j
public class DeliveryController {

  private final DeliveryService deliveryService;

  @PreAuthorize("hasAnyRole('COURIER','ADMIN')")
  @PostMapping("/{assignmentId}/pickup")
  public Map<String,Object> pickup(@PathVariable("assignmentId") UUID assignmentId) {
    // Note: This should use orderId, but keeping current logic for now
    deliveryService.markAsPickedUp(assignmentId);
    return Map.of("status","PICKED_UP");
  }

  @PreAuthorize("hasAnyRole('COURIER','ADMIN')")
  @PostMapping("/{assignmentId}/delivered")
  public Map<String,Object> delivered(@PathVariable("assignmentId") UUID assignmentId) {
    // Note: This should use orderId, but keeping current logic for now
    deliveryService.markAsDelivered(assignmentId);
    return Map.of("status","DELIVERED");
  }
}
