package com.example.food.delivery.web;

import com.example.food.delivery.domain.AssignmentEntity;
import com.example.food.delivery.mapping.DeliveryMapper;
import com.example.food.delivery.repo.AssignmentRepository;
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

  private final AssignmentRepository assignments;
  private final KafkaTemplate<String, Object> kafka;
  private final DeliveryMapper mapper;

  @PreAuthorize("hasAnyRole('COURIER','ADMIN')")
  @PostMapping("/{assignmentId}/pickup")
  public Map<String,Object> pickup(@PathVariable("assignmentId") UUID assignmentId) {
    log.info("DeliveryPickedUp assignmentId={}", assignmentId);
    AssignmentEntity a = assignments.findById(assignmentId).orElseThrow();
    a.setStatus("PICKED_UP");
    assignments.save(a);
    OrderPickedUpV1 evt = mapper.toPickedUp(a);
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.delivery.picked-up.v1", a.getOrderId().toString(), evt);
    rec.headers().add("eventType","fd.delivery.OrderPickedUpV1".getBytes());
    kafka.send(rec);
    return Map.of("status","PICKED_UP");
  }

  @PreAuthorize("hasAnyRole('COURIER','ADMIN')")
  @PostMapping("/{assignmentId}/delivered")
  public Map<String,Object> delivered(@PathVariable("assignmentId") UUID assignmentId) {
    log.info("DeliveryCompleted assignmentId={}", assignmentId);
    AssignmentEntity a = assignments.findById(assignmentId).orElseThrow();
    a.setStatus("DELIVERED");
    assignments.save(a);
    OrderDeliveredV1 evt = mapper.toDelivered(a);
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.delivery.delivered.v1", a.getOrderId().toString(), evt);
    rec.headers().add("eventType","fd.delivery.OrderDeliveredV1".getBytes());
    kafka.send(rec);
    return Map.of("status","DELIVERED");
  }
}
