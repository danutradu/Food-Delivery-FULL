package com.example.food.payment.service;

import fd.payment.PaymentRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentListener {

    private final PaymentService paymentService;

    @RetryableTopic(
            attempts = "${kafka.retry.attempts}",
            backoff = @Backoff(delayExpression = "${kafka.retry.delay}", multiplierExpression = "${kafka.retry.multiplier}", maxDelayExpression = "${kafka.retry.max-delay}"),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(id = "payment-requests", topics = "fd.payment.requested.v1", groupId = "payment-service")
    public void onPaymentRequested(PaymentRequestedV1 event) {
        paymentService.processPaymentRequest(event);
    }
}
