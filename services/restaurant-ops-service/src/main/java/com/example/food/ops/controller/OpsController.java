package com.example.food.ops.controller;

import com.example.food.ops.service.OpsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ops/orders")
@Slf4j
public class OpsController {

    private final OpsService opsService;

    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    @PostMapping("/{orderId}/accept")
    public Map<String, Object> accept(@PathVariable("orderId") UUID orderId, @RequestParam(name = "etaMinutes", defaultValue = "15") int etaMinutes) {
        // Note: This should use ticketId, but keeping curent logic for now
        opsService.acceptOrder(orderId, etaMinutes);
        return Map.of("status", "ACCEPTED");
    }

    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    @PostMapping("/{orderId}/reject")
    public Map<String, Object> reject(@PathVariable("orderId") UUID orderId, @RequestParam(name = "reason", defaultValue = "Out of stock") String reason) {
        // Note: This should use ticketId, but keeping current logic for now
        opsService.rejectOrder(orderId, reason);
        return Map.of("status", "REJECTED");
    }

    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    @PostMapping("/{orderId}/ready")
    public Map<String, Object> ready(@PathVariable("orderId") UUID orderId) {
        // Note: This should use ticketId, but keeping current logic for now
        opsService.markOrderReady(orderId);
        return Map.of("status", "READY");
    }
}
