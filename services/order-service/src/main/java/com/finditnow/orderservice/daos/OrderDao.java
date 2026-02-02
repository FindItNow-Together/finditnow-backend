package com.finditnow.orderservice.daos;

import com.finditnow.orderservice.entities.Order;
import com.finditnow.orderservice.repositories.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderDao {
    private final OrderRepository orderRepository;

    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public Optional<Order> findById(UUID id) {
        return orderRepository.findById(id);
    }

    public List<Order> findByUserId(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Order> findByShopId(Long shopId) {
        return orderRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }

    public Page<Order> findByShopId(Long shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findByShopId(shopId, pageable);
    }

    public Double calculateTotalEarnings(Long shopId) {
        return orderRepository.calculateTotalEarningsByShopId(shopId);
    }

    public List<String> findRecentProducts(Long shopId) {
        // Fetch top 5 recent unique products
        return orderRepository.findRecentProductNamesByShopId(shopId, PageRequest.of(0, 5));
    }

    @Transactional
    public Order updateOrderStatus(UUID orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional
    public Order updatePaymentStatus(UUID orderId, Order.PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setPaymentStatus(paymentStatus);
        return orderRepository.save(order);
    }
}
