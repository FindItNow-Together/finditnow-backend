package com.finditnow.deliveryservice.service;

import com.finditnow.deliveryservice.dto.DeliveryQuoteRequest;
import com.finditnow.deliveryservice.dto.DeliveryQuoteResponse;
import com.finditnow.deliveryservice.dto.InitiateDeliveryRequest;
import com.finditnow.deliveryservice.entity.Delivery;
import com.finditnow.deliveryservice.entity.DeliveryStatus;
import com.finditnow.deliveryservice.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    private static final double EARTH_RADIUS = 6371; // km

    public DeliveryQuoteResponse calculateQuote(DeliveryQuoteRequest request) {
        if (request.getShopLatitude() == null || request.getShopLongitude() == null ||
                request.getUserLatitude() == null || request.getUserLongitude() == null) {
            // Fallback or error
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

        // Round to 2 decimal places
        amount = Math.round(amount * 100.0) / 100.0;
        distance = Math.round(distance * 100.0) / 100.0;

        return new DeliveryQuoteResponse(amount, distance);
    }

    public Delivery initiateDelivery(InitiateDeliveryRequest request) {
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

        if (com.finditnow.deliveryservice.entity.DeliveryType.TAKEAWAY.equals(request.getType())) {
            delivery.setStatus(DeliveryStatus.DELIVERED);
        }

        // Mock assignment logic (in reality this would be complex)
        // For now, we leave it PENDING or Auto-Assign mock agent

        return deliveryRepository.save(delivery);
    }

    public Delivery getDeliveryByOrderId(UUID orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));
    }

    public Delivery getDeliveryById(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + deliveryId));
    }

    public Delivery updateStatus(UUID deliveryId, DeliveryStatus status) {
        Delivery delivery = getDeliveryById(deliveryId);
        delivery.setStatus(status);
        return deliveryRepository.save(delivery);
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
}
