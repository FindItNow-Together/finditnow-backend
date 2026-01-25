package com.finditnow.deliveryservice.service;

import com.finditnow.deliveryservice.dto.DeliveryQuoteRequest;
import com.finditnow.deliveryservice.dto.DeliveryQuoteResponse;
import com.finditnow.deliveryservice.dto.InitiateDeliveryRequest;
import com.finditnow.deliveryservice.entity.Delivery;
import com.finditnow.deliveryservice.entity.DeliveryStatus;
import com.finditnow.deliveryservice.entity.DeliveryType;
import com.finditnow.deliveryservice.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    private DeliveryQuoteRequest quoteRequest;
    private InitiateDeliveryRequest initiateRequest;

    @BeforeEach
    void setUp() {
        quoteRequest = DeliveryQuoteRequest.builder()
                .shopLatitude(12.9716)
                .shopLongitude(77.5946)
                .userLatitude(12.9716)
                .userLongitude(77.6046) // approx 1km away
                .build();

        initiateRequest = InitiateDeliveryRequest.builder()
                .orderId(UUID.randomUUID())
                .shopId(1L)
                .customerId(UUID.randomUUID())
                .type(DeliveryType.PARTNER)
                .amount(50.0)
                .pickupAddress("Shop Address")
                .deliveryAddress("User Address")
                .instructions("Handle with care")
                .build();
    }

    @Test
    void calculateQuote_ShouldReturnCorrectDistanceAndAmount() {
        // Same coords = 0 km
        DeliveryQuoteRequest sameLoc = DeliveryQuoteRequest.builder()
                .shopLatitude(12.9716).shopLongitude(77.5946)
                .userLatitude(12.9716).userLongitude(77.5946)
                .build();

        DeliveryQuoteResponse response = deliveryService.calculateQuote(sameLoc);

        assertEquals(0.0, response.getDistanceKm());
        assertEquals(20.0, response.getAmount()); // Base fee
    }

    @Test
    void initiateDelivery_ShouldSaveAndReturnDelivery() {
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> {
            Delivery d = invocation.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        Delivery delivery = deliveryService.initiateDelivery(initiateRequest);

        assertNotNull(delivery.getId());
        assertEquals(DeliveryStatus.PENDING, delivery.getStatus());
        assertEquals(DeliveryType.PARTNER, delivery.getType());
        assertEquals(initiateRequest.getOrderId(), delivery.getOrderId());

        verify(deliveryRepository, times(1)).save(any(Delivery.class));
    }

    @Test
    void initiateDelivery_Takeaway_ShouldSetStatusToDelivered() {
        initiateRequest.setType(DeliveryType.TAKEAWAY);

        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Delivery delivery = deliveryService.initiateDelivery(initiateRequest);

        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
        assertEquals(DeliveryType.TAKEAWAY, delivery.getType());
    }
}
