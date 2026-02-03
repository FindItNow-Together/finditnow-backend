package com.finditnow.orderservice.clients;

import com.finditnow.interservice.InterServiceClient;
import com.finditnow.interservice.JsonUtil;
import com.finditnow.orderservice.dtos.DeliveryQuoteResponse;
import com.finditnow.orderservice.dtos.InitiateDeliveryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${delivery.service.url:http://localhost:8086}")
    private String deliveryServiceUrl;
    private static final long QUOTE_CACHE_SECONDS = Duration.ofDays(15).toSeconds();

    public void initiateDelivery(InitiateDeliveryRequest request) {
        try {
            var res = InterServiceClient.call("delivery-service", "/deliveries/initiate", "POST", JsonUtil.toJson(request));
//            String url = deliveryServiceUrl + "/deliveries/initiate";
//            restTemplate.postForObject(url, request, Object.class);
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                log.info("Initiated delivery for order: {}", request.getOrderId());
            }else{
                throw new RuntimeException("Failed to initiate delivery: status code " + res.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to initiate delivery for order: {}", request.getOrderId(), e);
        }
    }

    public com.finditnow.orderservice.dtos.DeliveryQuoteResponse calculateQuote(
            com.finditnow.orderservice.dtos.DeliveryQuoteRequest request) {
        try {
            String deliveryQuoteReq = JsonUtil.toJson(request);

            var quoteRes = InterServiceClient.call("delivery-service", "/deliveries/calculate-quote", "POST", deliveryQuoteReq, true, QUOTE_CACHE_SECONDS);

            log.info("Delivery quote received: {}; for request: {}", quoteRes.body(), deliveryQuoteReq);

            return JsonUtil.fromJson(quoteRes.body(), DeliveryQuoteResponse.class);

//            String url = deliveryServiceUrl + "/deliveries/calculate-quote";
//            return restTemplate.postForObject(url, request,
//                    com.finditnow.orderservice.dtos.DeliveryQuoteResponse.class);
        } catch (Exception e) {
            log.error("Failed to calculate delivery quote", e);
            return new com.finditnow.orderservice.dtos.DeliveryQuoteResponse(0.0, 0.0);
        }
    }
}
