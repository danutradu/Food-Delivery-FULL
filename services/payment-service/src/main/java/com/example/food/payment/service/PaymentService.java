package com.example.food.payment.service;

import com.example.food.payment.mapper.PaymentMapper;
import com.example.food.payment.repository.PaymentRepository;
import fd.payment.PaymentRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository payments;
    private final KafkaTemplate<String, Object> kafka;
    private final PaymentMapper mapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public void processPaymentRequest(PaymentRequestedV1 event) {
        log.info("Processing payment request orderId={} amount{}", event.getOrderId(), event.getAmountCents());

        var orderId = event.getOrderId();
        var payment = payments.findByOrderId(orderId)
                .orElseGet(() -> mapper.fromRequested(event));

        payment.setAuthorizationCode("AUTH-" + Math.abs(secureRandom.nextInt()));
        payments.save(payment);

        var authorizedEvent = mapper.toAuthorized(payment);
        ProducerRecord<String, Object> record = new ProducerRecord<>("fd.payment.authorized.v1", event.getOrderId().toString(), authorizedEvent);
        record.headers().add("eventType", "fd.payment.PaymentAuthorizedV1".getBytes());
        kafka.send(record);
    }
}
