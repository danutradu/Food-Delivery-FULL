package com.example.food.payment.service;

import fd.payment.PaymentRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentListener {

    private final PaymentService paymentService;

    @KafkaListener(id = "payment-requests", topics = "fd.payment.requested.v1", groupId = "payment-service")
    public void onPaymentRequested(PaymentRequestedV1 event) {
        paymentService.processPaymentRequest(event);
    }
}
