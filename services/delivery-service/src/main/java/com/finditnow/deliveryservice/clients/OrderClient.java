package com.finditnow.deliveryservice.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class OrderClient {

    private final RestTemplate restTemplate;

    @Value("${order.service.url:http://localhost:8085}")
    private String orderServiceUrl;

    public OrderClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Updates the order status in order-service.
     * This is a best-effort call - failures are logged but don't throw exceptions.
     *
     * @param orderId The order ID to update
     * @param status  The new status (e.g., "DELIVERED", "CANCELLED")
     */
    public void updateOrderStatus(UUID orderId, String status) {
        try {
            String url = orderServiceUrl + "/orders/" + orderId + "/status";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("status", status);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            restTemplate.put(url, request);
            log.info("Successfully updated order {} status to {}", orderId, status);
        } catch (Exception e) {
            log.error("Failed to update order {} status to {}. Delivery status update will proceed.",
                    orderId, status, e);
        }
    }
}
