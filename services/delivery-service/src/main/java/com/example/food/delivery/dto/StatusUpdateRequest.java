package com.example.food.delivery.dto;

import com.example.food.delivery.model.AssignmentStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull AssignmentStatus status) {
}
