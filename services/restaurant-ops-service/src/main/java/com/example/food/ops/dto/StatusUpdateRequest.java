package com.example.food.ops.dto;

import com.example.food.ops.model.KitchenTicketStatus;

public record StatusUpdateRequest(KitchenTicketStatus status, Integer etaMinutes, String reason) {
}
