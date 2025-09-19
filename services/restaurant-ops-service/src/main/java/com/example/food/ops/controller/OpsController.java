package com.example.food.ops.controller;

import com.example.food.ops.dto.StatusUpdateRequest;
import com.example.food.ops.model.KitchenTicketEntity;
import com.example.food.ops.model.KitchenTicketStatus;
import com.example.food.ops.service.OpsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ops/orders")
@Slf4j
public class OpsController {

    private final OpsService opsService;

    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    @PatchMapping("/{orderId}/status")
    public KitchenTicketEntity updateStatus(@PathVariable UUID orderId, @RequestBody StatusUpdateRequest request) {
        return opsService.updateStatus(orderId, request);
    }

    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    @GetMapping
    public List<KitchenTicketEntity> getTickets(@RequestParam(required = false) KitchenTicketStatus status) {
        return opsService.getTickets(status);
    }

    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    @GetMapping("/{orderId}")
    public KitchenTicketEntity getTicket(@PathVariable UUID orderId) {
        return opsService.getTicket(orderId);
    }
}
