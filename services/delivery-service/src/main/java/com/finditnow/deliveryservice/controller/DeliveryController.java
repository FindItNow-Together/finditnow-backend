package com.finditnow.deliveryservice.controller;

import com.finditnow.deliveryservice.dto.DeliveryQuoteRequest;
import com.finditnow.deliveryservice.dto.DeliveryQuoteResponse;
import com.finditnow.deliveryservice.dto.InitiateDeliveryRequest;
import com.finditnow.deliveryservice.entity.Delivery;
import com.finditnow.deliveryservice.entity.DeliveryStatus;
import com.finditnow.deliveryservice.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/calculate-quote")
    public ResponseEntity<DeliveryQuoteResponse> calculateQuote(@RequestBody DeliveryQuoteRequest request) {
        return ResponseEntity.ok(deliveryService.calculateQuote(request));
    }

    @PostMapping("/initiate")
    public ResponseEntity<Delivery> initiateDelivery(@RequestBody InitiateDeliveryRequest request) {
        return ResponseEntity.ok(deliveryService.initiateDelivery(request));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Delivery> getDeliveryByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(deliveryService.getDeliveryByOrderId(orderId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Delivery> updateStatus(@PathVariable UUID id, @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, request.getStatus()));
    }

    // Tiny DTO for status update
    @lombok.Data
    public static class StatusUpdateRequest {
        private DeliveryStatus status;
    }
}
