package com.finditnow.deliveryservice.clients;

import com.finditnow.interservice.InterServiceClient;
import com.finditnow.interservice.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
            var res = InterServiceClient.call("order-service", "/orders/" + orderId + "/status", "PUT", JsonUtil.toJson(Map.of("status", status)));
            if (res.statusCode() >= 200 && res.statusCode() <= 300) {
                log.info("Successfully updated order {} status to {}", orderId, status);
            }else{
                throw new RuntimeException("Status code: " + res.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to update order {} status to {}. Delivery status update will proceed.",
                    orderId, status, e);
        }
    }
}
