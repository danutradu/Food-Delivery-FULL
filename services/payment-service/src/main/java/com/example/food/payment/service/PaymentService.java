package com.example.food.payment.service;

import com.example.food.common.outbox.OutboxService;
import com.example.food.payment.config.KafkaTopics;
import com.example.food.payment.model.PaymentStatus;
import com.example.food.payment.repository.PaymentRepository;
import com.example.food.payment.util.PaymentFactory;
import fd.payment.FeeRequestedV1;
import fd.payment.PaymentRequestedV1;
import fd.payment.RefundRequestedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OutboxService outboxService;
    private final KafkaTopics topics;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void processPaymentRequest(PaymentRequestedV1 event) {
        log.info("Processing payment request orderId={} amount={}", event.getOrderId(), event.getAmount());

        var orderId = event.getOrderId();

        // Check for existing SUCCESSFUL payment
        var existingAuthorized = paymentRepository.findByOrderIdAndAndStatus(orderId, PaymentStatus.AUTHORIZED);
        if (existingAuthorized.isPresent()) {
            log.debug("Payment already authorized for orderId={}", orderId);
            return; // Skip - already successfully paid
        }

        var payment = PaymentFactory.createPayment(event);
        payment.setStatus(PaymentStatus.PENDING);

        // Simulate payment gateway call
        if (simulatePaymentGateway(event.getAmount())) {
            payment.setStatus(PaymentStatus.AUTHORIZED);
            payment.setAuthorizationCode("AUTH-" + Math.abs(secureRandom.nextInt()));
            paymentRepository.save(payment);

            var authorizedEvent = PaymentFactory.createPaymentAuthorized(payment);
            outboxService.publish(topics.getPaymentAuthorized(), authorizedEvent.getOrderId().toString(), authorizedEvent);

            log.info("Payment authorized orderId={} authCode={}", orderId, payment.getAuthorizationCode());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Insufficient funds");
            paymentRepository.save(payment);

            var failedEvent = PaymentFactory.createPaymentFailed(payment);
            outboxService.publish(topics.getPaymentFailed(), failedEvent.getOrderId().toString(), failedEvent);

            log.warn("Payment failed orderId={} reason={}", orderId, payment.getFailureReason());
        }
    }

    private boolean simulatePaymentGateway(int amount) {
        // Simulate payment gateway - 80% success rate
        // Fail for amounts > 100 to simulate insufficient funds
        if (amount > 10000) {
            return false; // Simulate insufficient funds for large amounts
        }
        return secureRandom.nextInt(100) < 80; // 80% success rate
    }

    @Transactional
    public void processRefundRequest(RefundRequestedV1 event) {
        log.info("Processing refund request orderId={} amount={} reason={}", event.getOrderId(), event.getAmount(), event.getReason());

        var payment = paymentRepository.findByOrderIdAndAndStatus(event.getOrderId(), PaymentStatus.AUTHORIZED)
                .orElse(null);

        if (payment == null) {
            log.warn("No authorized payment found for refund orderId={}", event.getOrderId());
            return;
        }

        // Simulate refund processing
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        var refundCompletedEvent = PaymentFactory.createRefundCompleted(payment, event.getReason());
        outboxService.publish(topics.getRefundCompleted(), payment.getOrderId().toString(), refundCompletedEvent);

        log.info("Refund processed successfully orderId={} amount={}", event.getOrderId(), event.getAmount());
    }

    @Transactional
    public void processFeeRequest(FeeRequestedV1 event) {
        log.info("Processing fee request orderId={} fee={} reason={}", event.getOrderId(), event.getFee(), event.getReason());

        var payment = paymentRepository.findByOrderIdAndAndStatus(event.getOrderId(), PaymentStatus.AUTHORIZED)
                .orElse(null);

        if (payment == null) {
            log.warn("No authorized payment found for fee charging orderId={}", event.getOrderId());
            return;
        }

        // Simulate fee charging (TODO: instead of this it should be charging customer's card)
        var feeCharged = simulateFeeCharging(event.getFee());

        if (feeCharged) {
            log.info("Fee charged successfully orderId={} amount={} reason={}", event.getOrderId(), event.getFee(), event.getReason());

            var feeChargedEvent = PaymentFactory.createFeeCharged(event);
            outboxService.publish(topics.getFeeCharged(), feeChargedEvent.getOrderId().toString(), feeChargedEvent);
        } else {
            log.warn("Fee charging failed orderId={} amount={} reason={}", event.getOrderId(), event.getFee(), event.getReason());

            var feeFailedEvent = PaymentFactory.createFeeFailed(event, "Payment gateway error");
            outboxService.publish(topics.getFeeFailed(), feeFailedEvent.getOrderId().toString(), feeFailedEvent);
        }
    }

    private boolean simulateFeeCharging(int fee) {
        // Simulate fee charging - 95% success rate
        return secureRandom.nextInt(100) < 95;
    }
}
