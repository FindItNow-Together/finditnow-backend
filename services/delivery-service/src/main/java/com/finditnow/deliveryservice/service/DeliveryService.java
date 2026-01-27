package com.finditnow.deliveryservice.service;

import com.finditnow.deliveryservice.dto.*;
import com.finditnow.deliveryservice.entity.Delivery;
import com.finditnow.deliveryservice.entity.DeliveryStatus;
import com.finditnow.deliveryservice.entity.DeliveryType;
import com.finditnow.deliveryservice.repository.DeliveryRepository;
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

    private static final double EARTH_RADIUS = 6371; // km

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
        delivery.setStatus(DeliveryStatus.PENDING);

        if (DeliveryType.TAKEAWAY.equals(request.getType())) {
            delivery.setStatus(DeliveryStatus.DELIVERED);
        }

        Delivery savedDelivery = deliveryRepository.save(delivery);
        return mapToResponse(savedDelivery);
    }

    public DeliveryResponse getDeliveryByOrderId(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));
        return mapToResponse(delivery);
    }

    public DeliveryResponse getDeliveryById(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + deliveryId));
        return mapToResponse(delivery);
    }

    public DeliveryResponse updateStatus(UUID deliveryId, DeliveryStatus status) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + deliveryId));
        delivery.setStatus(status);
        Delivery updatedDelivery = deliveryRepository.save(delivery);
        return mapToResponse(updatedDelivery);
    }

    public PagedDeliveryResponse getDeliveriesByAgentId(
            UUID agentId, DeliveryStatus status, int page, int limit) {

        // Validate pagination parameters
        if (page < 0) page = 0;
        if (limit <= 0 || limit > 100) limit = 10;

        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());

        Page<Delivery> deliveryPage;

        if (status != null) {
            // Filter by specific status
            deliveryPage = deliveryRepository.findByAssignedAgentIdAndStatus(
                    agentId, status, pageable);
        } else {
            // Get all active deliveries (exclude DELIVERED and CANCELLED)
            deliveryPage = deliveryRepository.findByAssignedAgentIdAndStatusNotIn(
                    agentId,
                    List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED),
                    pageable);
        }

        List<DeliveryResponse> deliveryResponses = deliveryPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedDeliveryResponse.builder()
                .deliveries(deliveryResponses)
                .currentPage(deliveryPage.getNumber())
                .totalPages(deliveryPage.getTotalPages())
                .totalElements(deliveryPage.getTotalElements())
                .pageSize(deliveryPage.getSize())
                .hasNext(deliveryPage.hasNext())
                .hasPrevious(deliveryPage.hasPrevious())
                .build();
    }

    private double calculateDistanceIds(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

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