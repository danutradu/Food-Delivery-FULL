package com.example.food.payment.service;

import com.example.food.payment.mapper.PaymentMapper;
import com.example.food.payment.model.PaymentEntity;
import com.example.food.payment.model.PaymentStatus;
import com.example.food.payment.repository.PaymentRepository;
import fd.payment.PaymentRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, SpecificRecord> kafka;
    private final PaymentMapper paymentMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public void processPaymentRequest(PaymentRequestedV1 event) {
        log.info("Processing payment request orderId={} amount={}", event.getOrderId(), event.getAmountCents());

        var orderId = event.getOrderId();

        // Check for existing SUCCESSFUL payment
        var existingAuthorized = paymentRepository.findByOrderIdAndAndStatus(orderId, PaymentStatus.AUTHORIZED);
        if (existingAuthorized.isPresent()) {
            log.debug("Payment already authorized for orderId={}", orderId);
            return; // Skip - already successfully paid
        }

        var payment = paymentMapper.fromRequested(event);
        payment.setStatus(PaymentStatus.PENDING);

        // Simulate payment gateway call
        if (simulatePaymentGateway(event.getAmountCents())) {
            // Payment succeeded
            payment.setStatus(PaymentStatus.AUTHORIZED);
            payment.setAuthorizationCode("AUTH-" + Math.abs(secureRandom.nextInt()));
            paymentRepository.save(payment);

            log.info("Payment authorized orderId={} authCode={}", orderId, payment.getAuthorizationCode());
            publishPaymentAuthorized(payment, event.getEventId());
        } else {
            // Payment failed
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Insufficient funds");
            paymentRepository.save(payment);

            log.warn("Payment failed orderId={} reason={}", orderId, payment.getFailureReason());
            publishPaymentFailed(payment, event.getEventId());
        }
    }

    private boolean simulatePaymentGateway(int amountCents) {
        // Simulate payment gateway - 80% success rate
        // Fail for amounts > $100 to simulate insufficient funds
        if (amountCents > 10000) {
            return false; // Simulate insufficient funds for large amounts
        }
        return secureRandom.nextInt(100) < 80; // 80% success rate
    }

    private void publishPaymentAuthorized(PaymentEntity payment, UUID eventId) {
        var authorizedEvent = paymentMapper.toAuthorized(payment);
        ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>("fd.payment.authorized.v1", payment.getOrderId().toString(), authorizedEvent);
        record.headers().add("eventType", "fd.payment.PaymentAuthorizedV1".getBytes());
        record.headers().add("eventId", eventId.toString().getBytes());

        kafka.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish payment authorized event orderId={}", payment.getOrderId(), ex);
            } else {
                log.info("Published payment authorized event orderId={}", payment.getOrderId());
            }
        });
    }

    private void publishPaymentFailed(PaymentEntity payment, UUID eventId) {
        var failedEvent = paymentMapper.toFailed(payment);
        ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>("fd.payment.failed.v1", payment.getOrderId().toString(), failedEvent);
        record.headers().add("eventType", "fd.payment.PaymentFailedV1".getBytes());
        record.headers().add("eventId", eventId.toString().getBytes());

        kafka.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish payment failed event orderId={}", payment.getOrderId(), ex);
            } else {
                log.info("Published payment failed event orderId={} reason={}", payment.getOrderId(), payment.getFailureReason());
            }
        });
    }
}
