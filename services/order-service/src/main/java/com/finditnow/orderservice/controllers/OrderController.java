package com.finditnow.orderservice.controllers;

import com.finditnow.orderservice.dtos.CancelOrderRequest;
import com.finditnow.orderservice.dtos.CreateOrderFromCartRequest;
import com.finditnow.orderservice.dtos.OrderResponse;
import com.finditnow.orderservice.dtos.StatusUpdateRequest;
import com.finditnow.orderservice.services.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody StatusUpdateRequest request) {
        OrderResponse order = orderService.updateOrderStatus(orderId, request.getStatus());
        return ResponseEntity.ok(order);
    }

    /**
     * Customer cancels their own order. Requires CUSTOMER role and order ownership.
     * Allowed only when order status is CREATED, CONFIRMED, or PAID.
     */
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request,
            @RequestAttribute("userId") String userIdStr,
            @RequestAttribute("profile") String profile) {

        if (profile == null || !"CUSTOMER".equalsIgnoreCase(profile)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only customers can cancel orders"));
        }

        UUID userId = UUID.fromString(userIdStr);

        try {
            OrderResponse order = orderService.cancelOrderByCustomer(orderId, userId, request.getReason());
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null) {
                if (msg.contains("not found")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
                }
                if (msg.contains("Unauthorized") || msg.contains("does not belong")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", msg));
                }
                if (msg.contains("already cancelled") || msg.contains("cannot be cancelled") || msg.contains("at least 5")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
                }
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg != null ? msg : "Invalid request"));
        }
    }
}
