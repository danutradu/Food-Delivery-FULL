package com.example.food.delivery.service;

import com.example.food.delivery.model.AssignmentEntity;
import com.example.food.delivery.mapper.DeliveryMapper;
import com.example.food.delivery.repository.AssignmentRepository;
import fd.delivery.CourierAssignedV1;
import fd.delivery.DeliveryRequestedV1;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryListener {

  private final AssignmentRepository assignments;
  private final KafkaTemplate<String, Object> kafka;
  private final DeliveryMapper mapper;

  @KafkaListener(id="delivery-requests", topics="fd.delivery.requested.v1", groupId = "delivery-service")
  public void onDeliveryRequested(DeliveryRequestedV1 evt) {
    log.info("KAFKA RECV topic=fd.delivery.requested.v1 orderId={} restaurantId={}", evt.getOrderId(), evt.getRestaurantId());
    AssignmentEntity a = assignments.findByOrderId(evt.getOrderId())
        .orElseGet(() -> mapper.fromRequested(evt));
    assignments.save(a);

    CourierAssignedV1 out = mapper.toAssigned(a);
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.delivery.courier-assigned.v1", evt.getOrderId().toString(), out);
    rec.headers().add("eventType","fd.delivery.CourierAssignedV1".getBytes());
    kafka.send(rec);
  }
}
