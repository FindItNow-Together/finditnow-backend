package com.finditnow.orderservice.services;

import com.finditnow.orderservice.TestCartData;
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

        System.out.println("Total amount: " + totalAmount);

        // 4. Create order entity
        Order order = new Order();
        order.setUserId(userId);
        order.setShopId(cart.getShopId());
        order.setStatus(Order.OrderStatus.CREATED);
        order.setPaymentMethod(
                "online".equals(request.getPaymentMethod())
                        ? Order.PaymentMethod.ONLINE
                        : Order.PaymentMethod.CASH_ON_DELIVERY
        );
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setTotalAmount(totalAmount);
        order.setDeliveryAddressId(request.getAddressId());
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderItems(new ArrayList<>());

        System.out.println("ORDER BEFORE CART ITEM>>>>" + order);

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
        clearCart(request.getCartId(), userId);

        // 8. For COD, mark as confirmed
        if (savedOrder.getPaymentMethod() == Order.PaymentMethod.CASH_ON_DELIVERY) {
            savedOrder.setStatus(Order.OrderStatus.CONFIRMED);
            savedOrder = orderDao.save(savedOrder);
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

        log.info("Order {} confirmed after successful payment", orderId);
    }

    private CartDTO fetchCart(UUID cartId, UUID userId) {

        return TestCartData.getCartById(cartId);
//        try {
//            String url = CART_SERVICE_URL + "/api/cart/" + cartId;
//            // Add authentication headers as needed
//            RestTemplate restTemplate = new RestTemplate();
//
//            return restTemplate.getForObject(url, CartDTO.class);
//        } catch (Exception e) {
//            log.error("Failed to fetch cart: {}", cartId, e);
//            throw new RuntimeException("Failed to fetch cart");
//        }
    }

    private void clearCart(UUID cartId, UUID userId) {
//        try {
//            String url = CART_SERVICE_URL + "/api/cart/" + cartId;
//
//            RestTemplate restTemplate = new RestTemplate();
//
//            restTemplate.delete(url);
//            log.info("Cart {} cleared after order creation", cartId);
//        } catch (Exception e) {
//            log.warn("Failed to clear cart: {}", cartId, e);
//            // Don't fail order creation if cart clear fails
//        }
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
        response.setCreatedAt(order.getCreatedAt().toString());

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
}
