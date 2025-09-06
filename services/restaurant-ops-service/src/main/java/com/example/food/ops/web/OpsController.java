package com.example.food.ops.web;

import com.example.food.ops.domain.KitchenTicketEntity;
import com.example.food.ops.mapping.OpsMappers;
import com.example.food.ops.repo.KitchenTicketRepository;
import fd.restaurant.OrderReadyForPickupV1;
import fd.restaurant.RestaurantAcceptedV1;
import fd.restaurant.RestaurantRejectedV1;
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
@RequestMapping("/ops/orders")
@Slf4j
public class OpsController {

  private final KitchenTicketRepository tickets;
  private final KafkaTemplate<String, Object> kafka;
  private final OpsMappers mapper;

  @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
  @PostMapping("/{orderId}/accept")
  public Map<String,Object> accept(@PathVariable("orderId") UUID orderId, @RequestParam(name = "etaMinutes", defaultValue = "15") int etaMinutes) {
    log.info("RestaurantAccepted orderId={} etaMinutes={}", orderId, etaMinutes);
    KitchenTicketEntity t = tickets.findByOrderId(orderId).orElseThrow();
    t.setStatus("ACCEPTED");
    tickets.save(t);
    RestaurantAcceptedV1 evt = mapper.toAccepted(t, etaMinutes);
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.restaurant.accepted.v1", t.getOrderId().toString(), evt);
    rec.headers().add("eventType","fd.restaurant.RestaurantAcceptedV1".getBytes());
    kafka.send(rec);
    return Map.of("status","ACCEPTED");
  }

  @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
  @PostMapping("/{orderId}/reject")
  public Map<String,Object> reject(@PathVariable("orderId") UUID orderId, @RequestParam(name = "reason", defaultValue = "Out of stock") String reason) {
    log.info("RestaurantRejected orderId={} reason={}", orderId, reason);
    KitchenTicketEntity t = tickets.findByOrderId(orderId).orElseThrow();
    t.setStatus("REJECTED");
    tickets.save(t);
    RestaurantRejectedV1 evt = mapper.toRejected(t, reason);
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.restaurant.rejected.v1", t.getOrderId().toString(), evt);
    rec.headers().add("eventType","fd.restaurant.RestaurantRejectedV1".getBytes());
    kafka.send(rec);
    return Map.of("status","REJECTED");
  }

  @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
  @PostMapping("/{orderId}/ready")
  public Map<String,Object> ready(@PathVariable("orderId") UUID orderId) {
    log.info("RestaurantOrderReady orderId={}", orderId);
    KitchenTicketEntity t = tickets.findByOrderId(orderId).orElseThrow();
    t.setStatus("READY");
    tickets.save(t);
    OrderReadyForPickupV1 evt = mapper.toReady(t);
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.restaurant.order-ready.v1", t.getOrderId().toString(), evt);
    rec.headers().add("eventType","fd.restaurant.OrderReadyForPickupV1".getBytes());
    kafka.send(rec);
    return Map.of("status","READY");
  }
}
