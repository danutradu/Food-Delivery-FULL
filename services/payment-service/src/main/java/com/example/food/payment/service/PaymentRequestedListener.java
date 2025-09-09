package com.example.food.payment.service;

import com.example.food.payment.model.PaymentEntity;
import com.example.food.payment.mapping.PaymentMapper;
import com.example.food.payment.repository.PaymentRepository;
import fd.payment.PaymentAuthorizedV1;
import fd.payment.PaymentRequestedV1;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestedListener {

  private final PaymentRepository payments;
  private final KafkaTemplate<String, Object> kafka;
  private final PaymentMapper mapper;
  private final SecureRandom secureRandom = new SecureRandom();

  @KafkaListener(id="payment-requests", topics="fd.payment.requested.v1", groupId = "payment-service")
  @Transactional
  public void onPaymentRequested(PaymentRequestedV1 evt) {
    log.info("KAFKA RECV topic=fd.payment.requested.v1 orderId={} amount={}", evt.getOrderId(), evt.getAmountCents());
    UUID orderId = evt.getOrderId();
    PaymentEntity p = payments.findByOrderId(orderId).orElseGet(() -> mapper.fromRequested(evt));
    p.setAuthorizationCode("AUTH-" + Math.abs(secureRandom.nextInt()));
    payments.save(p);

    PaymentAuthorizedV1 out = mapper.toAuthorized(p);
    ProducerRecord<String,Object> record = new ProducerRecord<>("fd.payment.authorized.v1", evt.getOrderId().toString(), out);
    record.headers().add("eventType", "fd.payment.PaymentAuthorizedV1".getBytes());
    kafka.send(record);
  }
}
