package com.finditnow.deliveryservice.controller;

import com.finditnow.deliveryservice.dto.*;
import com.finditnow.deliveryservice.entity.DeliveryStatus;
import com.finditnow.deliveryservice.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/calculate-quote")
    public ResponseEntity<DeliveryQuoteResponse> calculateQuote(
            @Valid @RequestBody DeliveryQuoteRequest request) {
        return ResponseEntity.ok(deliveryService.calculateQuote(request));
    }

    @PostMapping("/initiate")
    public ResponseEntity<DeliveryResponse> initiateDelivery(
            @Valid @RequestBody InitiateDeliveryRequest request) {
        return ResponseEntity.ok(deliveryService.initiateDelivery(request));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryResponse> getDeliveryByOrder(
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(deliveryService.getDeliveryByOrderId(orderId));
    }

    /**
     * Cancel delivery by order ID (internal: called by order-service when customer cancels order).
     * Requires SERVICE role (service token).
     */
    @PutMapping("/order/{orderId}/cancel")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> cancelDeliveryByOrderId(@PathVariable UUID orderId) {
        DeliveryResponse response = deliveryService.cancelByOrderId(orderId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DELIVERY_AGENT', 'ADMIN')")
    public ResponseEntity<DeliveryResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, request.getStatus()));
    }

    /**
     * Get deliveries assigned to a specific agent with pagination and filtering
     *
     * @param userIdStr agent id
     * @param status    optional status filter
     * @param page      page number (0-indexed)
     * @param limit     page size
     * @return paginated list of deliveries
     */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<PagedDeliveryResponse> getMyDeliveries(
            @RequestAttribute("userId") String userIdStr,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {

        UUID agentId = UUID.fromString(userIdStr);
        PagedDeliveryResponse response = deliveryService.getDeliveriesByAgentId(
                agentId, status, page, limit);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<DeliveryResponse> acceptDelivery(
            @PathVariable UUID id,
            @RequestAttribute("userId") String userIdStr) {
        UUID agentId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(deliveryService.acceptDelivery(id, agentId));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<DeliveryResponse> completeDelivery(
            @PathVariable UUID id,
            @RequestAttribute("userId") String userIdStr) {
        UUID agentId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(deliveryService.completeDelivery(id, agentId));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<DeliveryResponse> cancelDelivery(
            @PathVariable UUID id,
            @RequestAttribute("userId") String userIdStr) {
        UUID agentId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(deliveryService.cancelDeliveryByAgent(id, agentId));
    }

    @PutMapping("/{id}/opt-out")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<DeliveryResponse> optOutDelivery(
            @PathVariable UUID id,
            @RequestAttribute("userId") String userIdStr) {
        UUID agentId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(deliveryService.optOutDelivery(id, agentId));
    }
}