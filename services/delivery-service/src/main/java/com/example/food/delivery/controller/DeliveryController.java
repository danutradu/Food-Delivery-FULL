package com.example.food.delivery.controller;

import com.example.food.delivery.dto.StatusUpdateRequest;
import com.example.food.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/delivery/assignments")
@Slf4j
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PreAuthorize("hasAnyRole('COURIER','ADMIN')")
    @PatchMapping("/{assignmentId}/status")
    public void updateStatus(@PathVariable("assignmentId") UUID assignmentId, @Valid @RequestBody StatusUpdateRequest request) {
        deliveryService.updateAssignmentStatus(assignmentId, request.status());
    }
}
