package com.finditnow.orderservice.clients;

import com.finditnow.orderservice.dtos.InitiateDeliveryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${delivery.service.url:http://localhost:8086}")
    private String deliveryServiceUrl;

    public void initiateDelivery(InitiateDeliveryRequest request) {
        try {
            String url = deliveryServiceUrl + "/deliveries/initiate";
            restTemplate.postForObject(url, request, Object.class);
            log.info("Initiated delivery for order: {}", request.getOrderId());
        } catch (Exception e) {
            log.error("Failed to initiate delivery for order: {}", request.getOrderId(), e);
        }
    }

    public com.finditnow.orderservice.dtos.DeliveryQuoteResponse calculateQuote(
            com.finditnow.orderservice.dtos.DeliveryQuoteRequest request) {
        try {
            String url = deliveryServiceUrl + "/deliveries/calculate-quote";
            return restTemplate.postForObject(url, request,
                    com.finditnow.orderservice.dtos.DeliveryQuoteResponse.class);
        } catch (Exception e) {
            log.error("Failed to calculate delivery quote", e);
            return new com.finditnow.orderservice.dtos.DeliveryQuoteResponse(0.0, 0.0);
        }
    }
}
