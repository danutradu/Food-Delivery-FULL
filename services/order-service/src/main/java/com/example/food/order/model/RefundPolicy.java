package com.example.food.order.model;

public record RefundPolicy(
        boolean shouldRefund,
        RefundType refundType,
        int feeCents,
        String reason
) {}
