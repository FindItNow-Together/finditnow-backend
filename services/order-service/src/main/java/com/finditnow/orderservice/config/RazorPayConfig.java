package com.finditnow.orderservice.config;

import com.finditnow.config.Config;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorPayConfig {
    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        String keyID = Config.get("RAZORPAY_API_KEY");
        String secret = Config.get("RAZORPAY_API_SECRET");

        return new RazorpayClient(keyID, secret);
    }
}
