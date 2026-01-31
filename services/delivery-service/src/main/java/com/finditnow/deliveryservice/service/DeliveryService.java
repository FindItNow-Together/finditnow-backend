package com.finditnow.deliveryservice.service;

import com.finditnow.deliveryservice.dto.*;
import com.finditnow.deliveryservice.entity.*;
import com.finditnow.deliveryservice.repository.DeliveryAgentRepository;
import com.finditnow.deliveryservice.repository.DeliveryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final AssignmentService assignmentService;
    private final com.finditnow.deliveryservice.clients.OrderClient orderClient;

    private static final double EARTH_RADIUS = 6371; // km

    /**
     * Calculates delivery charge and distance based on shop and user coordinates.
     *
     * This method is pure computation and does not touch persistence.
     */
    public DeliveryQuoteResponse calculateQuote(DeliveryQuoteRequest request) {

        if (request.getShopLatitude() == null || request.getShopLongitude() == null ||
                request.getUserLatitude() == null || request.getUserLongitude() == null) {
            return new DeliveryQuoteResponse(0.0, 0.0);
        }

        double distance = calculateDistanceIds(
                request.getShopLatitude(), request.getShopLongitude(),
                request.getUserLatitude(), request.getUserLongitude());

        double amount;
        if (distance < 5) {
            amount = 20.0;
        } else if (distance < 10) {
            amount = 40.0;
        } else {
            amount = 60.0 + (distance - 10) * 5.0;
        }

        amount = Math.round(amount * 100.0) / 100.0;
        distance = Math.round(distance * 100.0) / 100.0;

        return new DeliveryQuoteResponse(amount, distance);
    }

    /**
     * Initiates a delivery for an order.
     *
     * - Persists the delivery in CREATED state
     * - TAKEAWAY deliveries are immediately marked DELIVERED
     * - Attempts best-effort assignment for non-takeaway deliveries
     *
     * Assignment failure MUST NOT roll back delivery creation.
     */
    @Transactional
    public DeliveryResponse initiateDelivery(InitiateDeliveryRequest request) {

        Delivery delivery = new Delivery();
        delivery.setOrderId(request.getOrderId());
        delivery.setShopId(request.getShopId());
        delivery.setCustomerId(request.getCustomerId());
        delivery.setType(request.getType());
        delivery.setPickupAddress(request.getPickupAddress());
        delivery.setDeliveryAddress(request.getDeliveryAddress());
        delivery.setInstructions(request.getInstructions());
        delivery.setDeliveryCharge(request.getAmount());

        delivery.setStatus(DeliveryStatus.CREATED);

        if (DeliveryType.TAKEAWAY.equals(request.getType())) {
            delivery.setStatus(DeliveryStatus.DELIVERED);
        }

        Delivery savedDelivery = deliveryRepository.save(delivery);

        if (savedDelivery.getStatus() == DeliveryStatus.CREATED) {
            try {
                assignmentService.attemptAssignment();
            } catch (Exception e) {
                log.error(
                        "Delivery assignment failed for delivery {}, continuing",
                        savedDelivery.getId(), e);
            }
        }

        return mapToResponse(savedDelivery);
    }

    /**
     * Fetches delivery by order ID.
     */
    public DeliveryResponse getDeliveryByOrderId(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));
        return mapToResponse(delivery);
    }

    /**
     * Fetches delivery by delivery ID.
     */
    public DeliveryResponse getDeliveryById(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + deliveryId));
        return mapToResponse(delivery);
    }

    /**
     * Updates delivery status.
     *
     * Now syncs ALL status changes to order service, not just terminal states.
     * Status mapping:
     * - PENDING_ACCEPTANCE → CONFIRMED
     * - ASSIGNED → CONFIRMED
     * - PICKED_UP → PICKED_UP
     * - IN_TRANSIT → IN_TRANSIT
     * - DELIVERED → DELIVERED
     * - FAILED → FAILED (and re-pool delivery)
     * - CANCELLED → CANCELLED
     * - CANCELLED_BY_AGENT → CANCELLED
     *
     * Assignment failure MUST NOT roll back the status update.
     */
    @Transactional
    public DeliveryResponse updateStatus(UUID deliveryId, DeliveryStatus newStatus) {

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        delivery.setStatus(newStatus);
        deliveryRepository.save(delivery);

        // Sync status to order service based on delivery status
        syncOrderStatus(delivery, newStatus);

        // Handle terminal states: free agent and attempt reassignment
        if (isTerminalState(newStatus)) {
            freeUpAgent(delivery);

            // Special handling for FAILED status: re-pool the delivery
            if (newStatus == DeliveryStatus.FAILED) {
                rePoolFailedDelivery(delivery);
            }

            try {
                assignmentService.attemptAssignment();
            } catch (Exception e) {
                log.error(
                        "Re-assignment failed after delivery {} completion, continuing",
                        deliveryId, e);
            }
        }

        return mapToResponse(delivery);
    }

    /**
     * Syncs delivery status to order status
     */
    private void syncOrderStatus(Delivery delivery, DeliveryStatus deliveryStatus) {
        String orderStatus;

        switch (deliveryStatus) {
            case PENDING_ACCEPTANCE:
            case ASSIGNED:
                orderStatus = "CONFIRMED";
                break;
            case PICKED_UP:
                orderStatus = "PICKED_UP";
                break;
            case IN_TRANSIT:
                orderStatus = "IN_TRANSIT";
                break;
            case DELIVERED:
                orderStatus = "DELIVERED";
                break;
            case FAILED:
                orderStatus = "FAILED";
                break;
            case CANCELLED:
            case CANCELLED_BY_AGENT:
                orderStatus = "CANCELLED";
                break;
            default:
                // For CREATED, UNASSIGNED, keep order as CONFIRMED
                orderStatus = "CONFIRMED";
                break;
        }

        try {
            orderClient.updateOrderStatus(delivery.getOrderId(), orderStatus);
            log.info("Synced order {} status to {}", delivery.getOrderId(), orderStatus);
        } catch (Exception e) {
            log.error("Failed to sync order status for order {}", delivery.getOrderId(), e);
            // Don't throw - status sync failure shouldn't block delivery updates
        }
    }

    /**
     * Checks if delivery status is terminal (delivery is complete/done)
     */
    private boolean isTerminalState(DeliveryStatus status) {
        return status == DeliveryStatus.DELIVERED
                || status == DeliveryStatus.FAILED
                || status == DeliveryStatus.CANCELLED
                || status == DeliveryStatus.CANCELLED_BY_AGENT;
    }

    /**
     * Frees up the assigned agent
     */
    private void freeUpAgent(Delivery delivery) {
        if (delivery.getAssignedAgentId() != null) {
            DeliveryAgent agent = deliveryAgentRepository
                    .findById(delivery.getAssignedAgentId())
                    .orElse(null);

            if (agent != null) {
                agent.setStatus(DeliveryAgentStatus.AVAILABLE);
                agent.setCurrentDeliveryId(null);
                deliveryAgentRepository.save(agent);
                log.info("Agent {} is now available", agent.getAgentId());
            }
        }
    }

    /**
     * Re-pools a failed delivery for reassignment
     */
    private void rePoolFailedDelivery(Delivery delivery) {
        log.info("Re-pooling failed delivery {} for reassignment", delivery.getId());

        // Reset delivery to CREATED state and clear agent assignment
        delivery.setStatus(DeliveryStatus.CREATED);
        delivery.setAssignedAgentId(null);
        deliveryRepository.save(delivery);

        log.info("Delivery {} reset to CREATED and ready for reassignment", delivery.getId());
    }

    /**
     * Agent explicitly accepts a delivery assigned to them.
     * Changes status from PENDING_ACCEPTANCE to ASSIGNED.
     */
    @Transactional
    public DeliveryResponse acceptDelivery(UUID deliveryId, UUID agentId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + deliveryId));

        // Validate agent is assigned to this delivery
        if (delivery.getAssignedAgentId() == null || !delivery.getAssignedAgentId().equals(agentId)) {
            throw new RuntimeException("Unauthorized: This delivery is not assigned to you");
        }

        // Validate delivery is in PENDING_ACCEPTANCE state
        if (!DeliveryStatus.PENDING_ACCEPTANCE.equals(delivery.getStatus())) {
            throw new RuntimeException("Invalid state: Delivery must be in PENDING_ACCEPTANCE state");
        }

        // Agent has accepted, proceed to ASSIGNED status
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        Delivery updatedDelivery = deliveryRepository.save(delivery);

        log.info("Agent {} accepted delivery {}", agentId, deliveryId);

        // Order status remains CONFIRMED
        return mapToResponse(updatedDelivery);
    }

    /**
     * Completes a delivery assigned to an agent
     */
    public DeliveryResponse completeDelivery(UUID deliveryId, UUID agentId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + deliveryId));

        // Validate agent is assigned to this delivery
        if (delivery.getAssignedAgentId() == null || !delivery.getAssignedAgentId().equals(agentId)) {
            throw new RuntimeException("Unauthorized: You are not assigned to this delivery");
        }

        // Validate delivery is in valid state for completion (after acceptance)
        DeliveryStatus currentStatus = delivery.getStatus();
        if (!DeliveryStatus.ASSIGNED.equals(currentStatus)
                && !DeliveryStatus.PICKED_UP.equals(currentStatus)
                && !DeliveryStatus.IN_TRANSIT.equals(currentStatus)) {
            throw new RuntimeException(
                    "Invalid state: Delivery must be assigned, picked up, or in transit before completion");
        }

        // Update status to DELIVERED (this will sync to order via updateStatus)
        return updateStatus(deliveryId, DeliveryStatus.DELIVERED);
    }

    /**
     * Cancels a delivery by the assigned agent
     */
    public DeliveryResponse cancelDeliveryByAgent(UUID deliveryId, UUID agentId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + deliveryId));

        // Validate agent is assigned to this delivery
        if (delivery.getAssignedAgentId() == null || !delivery.getAssignedAgentId().equals(agentId)) {
            throw new RuntimeException("Unauthorized: You are not assigned to this delivery");
        }

        // Validate delivery is not already completed or delivered
        if (DeliveryStatus.DELIVERED.equals(delivery.getStatus()) ||
                DeliveryStatus.CANCELLED.equals(delivery.getStatus()) ||
                DeliveryStatus.CANCELLED_BY_AGENT.equals(delivery.getStatus()) ||
                DeliveryStatus.FAILED.equals(delivery.getStatus())) {
            throw new RuntimeException(
                    "Invalid state: Cannot cancel a completed, failed, or already cancelled delivery");
        }

        delivery.setStatus(DeliveryStatus.CANCELLED_BY_AGENT);
        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Delivery {} cancelled by agent {}", deliveryId, agentId);

        // Free up the agent
        DeliveryAgent agent = deliveryAgentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + agentId));
        agent.setStatus(DeliveryAgentStatus.AVAILABLE);
        agent.setCurrentDeliveryId(null);
        deliveryAgentRepository.save(agent);

        // Sync order status
        orderClient.updateOrderStatus(delivery.getOrderId(), "CANCELLED");

        // Try to assign next delivery to this or other agents
        try {
            assignmentService.attemptAssignment();
        } catch (Exception e) {
            log.error("Re-assignment failed after delivery cancellation, continuing", e);
        }

        return mapToResponse(updatedDelivery);
    }

    /**
     * Allows an agent to opt out of a delivery
     */
    public DeliveryResponse optOutDelivery(UUID deliveryId, UUID agentId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + deliveryId));

        // Validate agent is assigned to this delivery
        if (delivery.getAssignedAgentId() == null || !delivery.getAssignedAgentId().equals(agentId)) {
            throw new RuntimeException("Unauthorized: You are not assigned to this delivery");
        }

        // Validate delivery is not completed, delivered, cancelled, or failed
        if (DeliveryStatus.DELIVERED.equals(delivery.getStatus()) ||
                DeliveryStatus.CANCELLED.equals(delivery.getStatus()) ||
                DeliveryStatus.CANCELLED_BY_AGENT.equals(delivery.getStatus()) ||
                DeliveryStatus.FAILED.equals(delivery.getStatus())) {
            throw new RuntimeException("Invalid state: Cannot opt out of a completed, failed, or cancelled delivery");
        }

        // Track this agent as opted-out for this delivery
        if (delivery.getOptedOutAgentIds() == null) {
            delivery.setOptedOutAgentIds(new java.util.HashSet<>());
        }
        delivery.getOptedOutAgentIds().add(agentId);

        // Remove agent assignment and set status to unassigned
        delivery.setAssignedAgentId(null);
        delivery.setStatus(DeliveryStatus.UNASSIGNED);
        Delivery updatedDelivery = deliveryRepository.save(delivery);

        // Free up the agent
        DeliveryAgent agent = deliveryAgentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + agentId));
        agent.setStatus(DeliveryAgentStatus.AVAILABLE);
        agent.setCurrentDeliveryId(null);
        deliveryAgentRepository.save(agent);

        log.info("Agent {} opted out of delivery {}", agentId, deliveryId);

        // Try to assign this delivery or others to available agents
        try {
            assignmentService.attemptAssignment();
        } catch (Exception e) {
            log.error("Re-assignment failed after agent opt-out, continuing", e);
        }

        return mapToResponse(updatedDelivery);
    }

    /**
     * Returns paginated deliveries for a given agent.
     *
     * If status is null, only active deliveries are returned.
     */
    public PagedDeliveryResponse getDeliveriesByAgentId(
            UUID agentId, DeliveryStatus status, int page, int limit) {

        if (page < 0)
            page = 0;
        if (limit <= 0 || limit > 100)
            limit = 10;

        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());

        Page<Delivery> deliveryPage;

        if (status != null) {
            deliveryPage = deliveryRepository.findByAssignedAgentIdAndStatus(
                    agentId, status, pageable);
        } else {
            deliveryPage = deliveryRepository.findByAssignedAgentIdAndStatusNotIn(
                    agentId,
                    List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED),
                    pageable);
        }

        List<DeliveryResponse> deliveries = deliveryPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedDeliveryResponse.builder()
                .deliveries(deliveries)
                .currentPage(deliveryPage.getNumber())
                .totalPages(deliveryPage.getTotalPages())
                .totalElements(deliveryPage.getTotalElements())
                .pageSize(deliveryPage.getSize())
                .hasNext(deliveryPage.hasNext())
                .hasPrevious(deliveryPage.hasPrevious())
                .build();
    }

    /**
     * Calculates Haversine distance between two latitude/longitude points.
     */
    private double calculateDistanceIds(
            double lat1, double lon1, double lat2, double lon2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    /**
     * Maps Delivery entity to API response.
     */
    private DeliveryResponse mapToResponse(Delivery delivery) {

        return DeliveryResponse.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrderId())
                .shopId(delivery.getShopId())
                .customerId(delivery.getCustomerId())
                .assignedAgentId(delivery.getAssignedAgentId())
                .status(delivery.getStatus())
                .type(delivery.getType())
                .pickupAddress(delivery.getPickupAddress())
                .deliveryAddress(delivery.getDeliveryAddress())
                .instructions(delivery.getInstructions())
                .deliveryCharge(delivery.getDeliveryCharge())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }
}
