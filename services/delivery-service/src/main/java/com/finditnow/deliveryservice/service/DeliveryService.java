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
     * If a delivery reaches a terminal state:
     * - DELIVERED
     * - FAILED
     * - CANCELLED
     *
     * Then:
     * - Agent capacity is freed
     * - Best-effort reassignment is attempted
     *
     * Assignment failure MUST NOT roll back the status update.
     */
    @Transactional
    public DeliveryResponse updateStatus(UUID deliveryId, DeliveryStatus newStatus) {

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        delivery.setStatus(newStatus);
        deliveryRepository.save(delivery);

        if (newStatus == DeliveryStatus.DELIVERED
                || newStatus == DeliveryStatus.FAILED
                || newStatus == DeliveryStatus.CANCELLED) {

            if (delivery.getAssignedAgentId() != null) {
                DeliveryAgent agent = deliveryAgentRepository
                        .findById(delivery.getAssignedAgentId())
                        .orElseThrow();

                agent.setStatus(DeliveryAgentStatus.AVAILABLE);
                agent.setCurrentDeliveryId(null);
                deliveryAgentRepository.save(agent);
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
     * Completes a delivery assigned to an agent
     */
    public DeliveryResponse completeDelivery(UUID deliveryId, UUID agentId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + deliveryId));

        // Validate agent is assigned to this delivery
        if (delivery.getAssignedAgentId() == null || !delivery.getAssignedAgentId().equals(agentId)) {
            throw new RuntimeException("Unauthorized: You are not assigned to this delivery");
        }

        // Validate delivery is in valid state for completion
        if (!DeliveryStatus.PICKED_UP.equals(delivery.getStatus())) {
            throw new RuntimeException("Invalid state: Delivery must be picked up before completion");
        }

        delivery.setStatus(DeliveryStatus.DELIVERED);
        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Delivery {} completed by agent {}", deliveryId, agentId);
        return mapToResponse(updatedDelivery);
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
                DeliveryStatus.CANCELLED_BY_AGENT.equals(delivery.getStatus())) {
            throw new RuntimeException("Invalid state: Cannot cancel a completed or already cancelled delivery");
        }

        delivery.setStatus(DeliveryStatus.CANCELLED_BY_AGENT);
        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Delivery {} cancelled by agent {}", deliveryId, agentId);
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

        // Validate delivery is not completed, delivered, or cancelled
        if (DeliveryStatus.DELIVERED.equals(delivery.getStatus()) ||
                DeliveryStatus.CANCELLED.equals(delivery.getStatus()) ||
                DeliveryStatus.CANCELLED_BY_AGENT.equals(delivery.getStatus())) {
            throw new RuntimeException("Invalid state: Cannot opt out of a completed or cancelled delivery");
        }

        // Remove agent assignment and set status to unassigned
        delivery.setAssignedAgentId(null);
        delivery.setStatus(DeliveryStatus.UNASSIGNED);
        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Agent {} opted out of delivery {}", agentId, deliveryId);
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
