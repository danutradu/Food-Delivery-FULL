package com.example.food.payment.service;

import com.example.food.payment.config.StandardRetryableTopic;
import fd.payment.FeeRequestedV1;
import fd.payment.PaymentRequestedV1;
import fd.payment.RefundRequestedV1;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentListener {

    private final PaymentService paymentService;

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.payment-requested}", groupId = "${kafka.consumer.group-id}")
    public void onPaymentRequested(PaymentRequestedV1 event) {
        paymentService.processPaymentRequest(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.refund-requested}", groupId = "${kafka.consumer.group-id}")
    public void onRefundRequested(RefundRequestedV1 event) {
        paymentService.processRefundRequest(event);
    }

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.fee-requested}", groupId = "${kafka.consumer.group-id}")
    public void onFeeRequested(FeeRequestedV1 event) {
        paymentService.processFeeRequest(event);
    }
}
