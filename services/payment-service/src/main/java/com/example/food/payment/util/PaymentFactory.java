package com.example.food.payment.util;

import com.example.food.payment.model.PaymentEntity;
import fd.payment.*;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class PaymentFactory {

    public PaymentEntity createPayment(PaymentRequestedV1 req) {
        var payment = new PaymentEntity();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(req.getOrderId());
        payment.setAmountCents(req.getAmountCents());
        payment.setCurrency(req.getCurrency());
        payment.setCreatedAt(Instant.now());
        return payment;
    }

    public PaymentAuthorizedV1 createPaymentAuthorized(PaymentEntity payment) {
        return new PaymentAuthorizedV1(
                UUID.randomUUID(),
                Instant.now(),
                payment.getOrderId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getAuthorizationCode()
        );
    }

    public PaymentFailedV1 createPaymentFailed(PaymentEntity payment) {
        return new PaymentFailedV1(
                UUID.randomUUID(),
                Instant.now(),
                payment.getOrderId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getFailureReason()
        );
    }

    public RefundCompletedV1 createRefundCompleted(PaymentEntity payment, String reason) {
        return new RefundCompletedV1(
                UUID.randomUUID(),
                Instant.now(),
                payment.getOrderId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                reason
        );
    }

    public FeeChargedV1 createFeeCharged(FeeRequestedV1 feeRequested) {
        return new FeeChargedV1(
                UUID.randomUUID(),
                Instant.now(),
                feeRequested.getOrderId(),
                feeRequested.getFeeCents(),
                feeRequested.getCurrency(),
                feeRequested.getReason()
        );
    }

    public FeeFailedV1 createFeeFailed(FeeRequestedV1 event, String failureReason) {
        return new FeeFailedV1(
                UUID.randomUUID(),
                Instant.now(),
                event.getOrderId(),
                event.getFeeCents(),
                event.getCurrency(),
                event.getReason(),
                failureReason
        );
    }
}
