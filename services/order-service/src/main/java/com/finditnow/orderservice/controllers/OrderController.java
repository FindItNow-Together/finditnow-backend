package com.finditnow.orderservice.controllers;

import com.finditnow.orderservice.dtos.CreateOrderFromCartRequest;
import com.finditnow.orderservice.dtos.OrderResponse;
import com.finditnow.orderservice.services.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {
    public final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/from-cart")
    public ResponseEntity<OrderResponse> createOrderFromCart(
            @RequestBody CreateOrderFromCartRequest request,
            @RequestAttribute("userId") String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        OrderResponse order = orderService.createOrderFromCart(request, userId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            @RequestAttribute("userId") String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        OrderResponse order = orderService.getOrder(orderId, userId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @RequestAttribute("userId") String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        List<OrderResponse> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/quote")
    public ResponseEntity<com.finditnow.orderservice.dtos.DeliveryQuoteResponse> getQuote(
            @RequestParam Long shopId,
            @RequestParam UUID addressId) {
        return ResponseEntity.ok(orderService.getDeliveryQuote(shopId, addressId));
    }
}
