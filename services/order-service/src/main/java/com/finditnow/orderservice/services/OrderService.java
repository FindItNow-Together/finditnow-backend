package com.finditnow.orderservice.services;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final RazorpayClient razorpayClient;

    public OrderService(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    public Order createOrder(int amount) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100);
        orderRequest.put("currency", "INR");

        return razorpayClient.orders.create(orderRequest);
    }
}
