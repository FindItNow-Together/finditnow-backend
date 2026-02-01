package com.finditnow.deliveryservice.controller;

import com.finditnow.deliveryservice.dto.CreateDeliveryAgentRequest;
import com.finditnow.deliveryservice.dto.UpdateAgentStatusRequest;
import com.finditnow.deliveryservice.entity.DeliveryAgent;
import com.finditnow.deliveryservice.entity.DeliveryAgentStatus;
import com.finditnow.deliveryservice.service.DeliveryAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/delivery-agent")
@RequiredArgsConstructor
public class DeliveryAgentController {

    private final DeliveryAgentService deliveryAgentService;

    /**
     * Inter-service call from User Service
     * Creates a delivery agent in OFFLINE state
     */
    @PostMapping("/add")
    public ResponseEntity<Void> createAgent(@RequestBody CreateDeliveryAgentRequest request) {
        deliveryAgentService.createAgent(request.getAgentId(), request.getZone());
        return ResponseEntity.ok().build();
    }

    /**
     * Agent updates their availability
     */
    @PutMapping("/{agentId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID agentId,
            @RequestBody UpdateAgentStatusRequest request
    ) {
        deliveryAgentService.updateStatus(agentId, request.getStatus());
        return ResponseEntity.ok().build();
    }

    /**
     * Agent updates their availability
     */
    @PutMapping("/my-status")
    public ResponseEntity<DeliveryAgentStatus> updateMyStatus(
            @RequestAttribute("userId") String userIdStr,
            @RequestBody UpdateAgentStatusRequest request
    ) {
        return ResponseEntity.ok(deliveryAgentService.updateStatus(UUID.fromString(userIdStr), request.getStatus()));
    }

    /**
     * Optional: fetch agent info
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<DeliveryAgent> getAgent(@PathVariable UUID agentId) {
        return ResponseEntity.ok(deliveryAgentService.getAgent(agentId));
    }

    @GetMapping("/my-status")
    public ResponseEntity<DeliveryAgentStatus> getMyStatus(@RequestAttribute("userId") String userIdStr) {
        return ResponseEntity.ok(deliveryAgentService.getAgentStatus(UUID.fromString(userIdStr)));
    }
}

