package com.finditnow.orderservice.services;

import com.finditnow.interservice.InterServiceClient;
import com.finditnow.interservice.JsonUtil;
import com.finditnow.orderservice.clients.DeliveryClient;
import com.finditnow.orderservice.daos.OrderDao;
import com.finditnow.orderservice.daos.PaymentDao;
import com.finditnow.orderservice.dtos.*;
import com.finditnow.orderservice.entities.Order;
import com.finditnow.orderservice.entities.OrderItem;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderDao orderDao;
    private final PaymentDao paymentDao;
    private final DeliveryClient deliveryClient;

    // This should be configured via application.properties
    private static final String CART_SERVICE_URL = "http://localhost:8081";

    @Transactional
    public OrderResponse createOrderFromCart(CreateOrderFromCartRequest request, UUID userId) {
        // 1. Fetch cart from cart service
        CartDTO cart = fetchCart(request.getCartId(), userId);

        if (cart == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty or not found");
        }

        // 2. Validate cart belongs to user
        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("Cart does not belong to user");
        }

        // 3. Calculate total
        double totalAmount = cart.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        // 3.1 Calculate Delivery Charge
        double deliveryCharge = 0.0;
        if (!"TAKEAWAY".equalsIgnoreCase(request.getDeliveryType())) {
            // TODO: Get actual lat/long from Shop and User Address
            DeliveryQuoteResponse quote = deliveryClient.calculateQuote(
                    DeliveryQuoteRequest.builder()
                            .shopLatitude(0.0).shopLongitude(0.0)
                            .userLatitude(0.0).userLongitude(0.0)
                            .build());
            deliveryCharge = quote.getAmount();
        }
        totalAmount += deliveryCharge;

        // 4. Create order entity
        Order order = new Order();
        order.setUserId(userId);
        order.setShopId(cart.getShopId());
        order.setStatus(Order.OrderStatus.CREATED);
        order.setPaymentMethod(
                "online".equals(request.getPaymentMethod())
                        ? Order.PaymentMethod.ONLINE
                        : Order.PaymentMethod.CASH_ON_DELIVERY);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setTotalAmount(totalAmount);
        order.setDeliveryAddressId(request.getAddressId());
        order.setDeliveryCharge(deliveryCharge);
        order.setInstructions(request.getInstructions());
        order.setDeliveryType(request.getDeliveryType() != null ? request.getDeliveryType() : "PARTNER");
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderItems(new ArrayList<>());

        // 5. Create order items
        for (CartItemDTO cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(cartItem.getProductName());
            orderItem.setPriceAtOrder(cartItem.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            order.getOrderItems().add(orderItem);
        }

        // 6. Save order
        Order savedOrder = orderDao.save(order);

        // 7. Clear cart (call cart service)
        consumeCart(request.getCartId(), userId);

        // 8. For COD, mark as confirmed
        if (savedOrder.getPaymentMethod() == Order.PaymentMethod.CASH_ON_DELIVERY) {
            savedOrder.setStatus(Order.OrderStatus.CONFIRMED);
            // savedOrder = orderDao.save(savedOrder);

            // Initiate Delivery for COD
            initiateDelivery(savedOrder);
        }

        return mapToOrderResponse(savedOrder);
    }

    public OrderResponse getOrder(UUID orderId, UUID userId) {
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify order belongs to user (or is shop owner - implement shop check)
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to order");
        }

        return mapToOrderResponse(order);
    }

    public List<OrderResponse> getUserOrders(UUID userId) {
        return orderDao.findByUserId(userId).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void confirmOrderPayment(UUID orderId) {
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderDao.save(order);

        // Initiate Delivery for Online Payment
        initiateDelivery(order);

        log.info("Order {} confirmed after successful payment", orderId);
    }

    public DeliveryQuoteResponse getDeliveryQuote(Long shopId, UUID addressId) {
        // TODO: Fetch Shop and User Address to get real Lat/Long
        // For now, mocking coordinates
        return deliveryClient.calculateQuote(
                DeliveryQuoteRequest.builder()
                        .shopLatitude(0.0).shopLongitude(0.0)
                        .userLatitude(0.0).userLongitude(0.0)
                        .build());
    }

    private CartDTO fetchCart(UUID cartId, UUID userId) {

        //call cart service for fetching the cart details
        try {
            var res = InterServiceClient.call("shop-service", "/cart/" + cartId.toString(), "GET", null);

            return JsonUtil.fromJson(res.body(), CartDTO.class);
        } catch (Exception e) {
            log.error("Error while fetching cart for cartId: {}", cartId, e);
            throw new RuntimeException("failed in fetching the cart for cartId: " + cartId);
        }

//         return TestCartData.getCartById(cartId);
    }

    private void consumeCart(UUID cartId, UUID userId) {
        //call cart service for clearing the cart
        try {
            InterServiceClient.call("shop-service", "/cart/" + cartId.toString() + "/internal/consume", "DELETE", null);
        } catch (Exception e) {
            log.error("Error while fetching cart for cartId: {}", cartId, e);
            throw new RuntimeException("failed in fetching the cart for cartId: " + cartId);
        }
    }

    /**
     * Customer cancels their own order. Allowed only when status is CREATED, CONFIRMED, or PAID.
     * Propagates cancellation to Delivery service. If payment was PAID, sets payment status to REFUND_PENDING.
     */
    @Transactional
    public OrderResponse cancelOrderByCustomer(UUID orderId, UUID userId, String reason) {
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: order does not belong to you");
        }

        Order.OrderStatus status = order.getStatus();
        if (status == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled");
        }
        if (status == Order.OrderStatus.OUT_FOR_DELIVERY
                || status == Order.OrderStatus.DELIVERED
                || status == Order.OrderStatus.PACKED
                || status == Order.OrderStatus.PICKED_UP
                || status == Order.OrderStatus.IN_TRANSIT
                || status == Order.OrderStatus.FAILED) {
            throw new RuntimeException("Order cannot be cancelled in current state");
        }

        if (reason == null || reason.trim().length() < 5) {
            throw new RuntimeException("Cancellation reason must be at least 5 characters");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledBy(Order.CancelledBy.CUSTOMER);
        order.setCancellationReason(reason.trim());
        order.setCancelledAt(LocalDateTime.now());

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            order.setPaymentStatus(Order.PaymentStatus.REFUND_PENDING);
            // TODO: Integrate with payment gateway to trigger refund when ready
        }

        Order savedOrder = orderDao.save(order);

        try {
            InterServiceClient.call("delivery-service", "/deliveries/order/" + orderId + "/cancel", "PUT", "{}");
            log.info("Delivery cancelled for order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to cancel delivery for order {}; order cancellation not rolled back", orderId, e);
        }

        return mapToOrderResponse(savedOrder);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setShopId(order.getShopId());
        response.setStatus(order.getStatus().name().toLowerCase());
        response.setPaymentMethod(order.getPaymentMethod().name().toLowerCase());
        response.setPaymentStatus(order.getPaymentStatus().name().toLowerCase());
        response.setTotalAmount(order.getTotalAmount());
        response.setDeliveryAddressId(order.getDeliveryAddressId());
        response.setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);

        if (order.getCancelledBy() != null) {
            response.setCancelledBy(order.getCancelledBy().name().toLowerCase());
            response.setCancellationReason(order.getCancellationReason());
            response.setCancelledAt(order.getCancelledAt() != null ? order.getCancelledAt().toString() : null);
        }

        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> {
                    OrderItemResponse itemResponse = new OrderItemResponse();
                    itemResponse.setId(item.getId());
                    itemResponse.setProductId(item.getProductId());
                    itemResponse.setProductName(item.getProductName());
                    itemResponse.setPriceAtOrder(item.getPriceAtOrder());
                    itemResponse.setQuantity(item.getQuantity());
                    return itemResponse;
                })
                .collect(Collectors.toList());

        response.setItems(items);
        return response;
    }

    private void initiateDelivery(Order order) {
        // TODO: Fetch Shop Address and Customer Address properly
        String placeholderAddress = "To be fetched address";

        InitiateDeliveryRequest request = InitiateDeliveryRequest.builder()
                .orderId(order.getId())
                .shopId(order.getShopId())
                .customerId(order.getUserId())
                .type(order.getDeliveryType())
                .amount(order.getDeliveryCharge())
                .pickupAddress(placeholderAddress) // We need to fetch shop address
                .deliveryAddress(order.getDeliveryAddressId().toString()) // Ideally fetch address text
                .instructions(order.getInstructions())
                .build();

        deliveryClient.initiateDelivery(request);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, String statusStr) {
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(statusStr);
            order.setStatus(newStatus);
            Order updatedOrder = orderDao.save(order);
            log.info("Order {} status updated to {}", orderId, statusStr);
            return mapToOrderResponse(updatedOrder);
        } catch (IllegalArgumentException e) {
            log.error("Invalid order status: {}", statusStr);
            throw new RuntimeException("Invalid order status: " + statusStr);
        }
    }
}
