package com.finditnow.orderservice.services;

import com.finditnow.config.Config;
import com.finditnow.orderservice.daos.OrderDao;
import com.finditnow.orderservice.daos.PaymentDao;
import com.finditnow.orderservice.dtos.InitiatePaymentRequest;
import com.finditnow.orderservice.dtos.PaymentInitiationResponse;
import com.finditnow.orderservice.entities.Order;
import com.finditnow.orderservice.entities.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {
    private final PaymentDao paymentDao;
    private final RazorpayClient razorpayClient;
    private final OrderDao orderDao;
    private final OrderService orderService;
    private final String razorpayKey;

    public PaymentService(RazorpayClient razorpayClient, PaymentDao paymentDao, OrderDao orderDao, OrderService orderService) throws RazorpayException {
        this.razorpayClient = razorpayClient;
        this.razorpayKey = Config.get("RAZORPAY_API_KEY");
        this.paymentDao = paymentDao;
        this.orderDao = orderDao;
        this.orderService = orderService;
    }

    @Transactional
    public PaymentInitiationResponse initiatePayment(InitiatePaymentRequest request) {
        // 1. Get order
        Order order = orderDao.findById(request.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found"));

        // 2. Validate order can accept payment
        if (order.getPaymentMethod() != Order.PaymentMethod.ONLINE) {
            throw new RuntimeException("Order is not configured for online payment");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new RuntimeException("Order is already paid");
        }

        try {
            // 3. Create Razorpay order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int) (order.getTotalAmount() * 100)); // Convert to paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", order.getId().toString());

            JSONObject notes = new JSONObject();
            notes.put("order_id", order.getId().toString());
            notes.put("user_id", order.getUserId().toString());
            orderRequest.put("notes", notes);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            // 4. Create payment record
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentMode(Payment.PaymentMode.UPI); // Will be updated based on actual payment
            payment.setProvider(Payment.Provider.RAZORPAY);
            payment.setProviderPaymentId(razorpayOrder.get("id"));
            payment.setAmount(order.getTotalAmount());
            payment.setStatus(Payment.Status.INITIATED);

            Payment savedPayment = paymentDao.save(payment);

            // 5. Return response for frontend
            PaymentInitiationResponse response = new PaymentInitiationResponse();
            response.setRazorpayKey(razorpayKey);
            response.setAmount((int) (order.getTotalAmount() * 100));
            response.setRazorpayOrderId(razorpayOrder.get("id"));
            response.setPaymentId(savedPayment.getId());

            log.info("Payment initiated for order: {}, razorpay order: {}", order.getId(), razorpayOrder.get("id"));

            return response;

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for order: {}", order.getId(), e);
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    /**
     * Handle Razorpay webhook events
     * Webhook events: payment.authorized, payment.captured, payment.failed, etc.
     */
    @Transactional
    public void handleRazorpayWebhook(String webhookBody, String receivedSignature) {
        // 1. Verify webhook signature
        if (!verifyWebhookSignature(webhookBody, receivedSignature)) {
            log.error("Webhook signature verification failed");
            throw new RuntimeException("Invalid webhook signature");
        }

        // 2. Parse webhook body
        JSONObject webhookJson = new JSONObject(webhookBody);

        // 3. Extract event details
        String event = webhookJson.getString("event");
        JSONObject payloadData = webhookJson.getJSONObject("payload");
        JSONObject paymentEntity = payloadData.getJSONObject("payment");
        JSONObject paymentEntityData = paymentEntity.getJSONObject("entity");

        String razorpayPaymentId = paymentEntityData.getString("id");
        String razorpayOrderId = paymentEntityData.getString("order_id");
        String status = paymentEntityData.getString("status");
        String method = paymentEntityData.optString("method", "upi");

        log.info("Processing webhook event: {}, payment: {}, order: {}, status: {}",
                event, razorpayPaymentId, razorpayOrderId, status);

        // 4. Find payment record
        Payment payment = paymentDao.findByProviderPaymentId(razorpayOrderId)
                .orElse(null);

        if (payment == null) {
            log.warn("Payment not found for razorpay order: {}", razorpayOrderId);
            return;
        }

        // 5. Update payment based on event
        switch (event) {
            case "payment.authorized":
                handlePaymentAuthorized(payment, razorpayPaymentId, method);
                break;
            case "payment.captured":
                handlePaymentCaptured(payment, razorpayPaymentId, method);
                break;
            case "payment.failed":
                handlePaymentFailed(payment, razorpayPaymentId);
                break;
            default:
                log.info("Unhandled webhook event: {}", event);
        }
    }

    @Transactional
    public void handlePaymentCallback(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        // 1. Find payment by razorpay order ID
        Payment payment = paymentDao.findByProviderPaymentId(razorpayOrderId).orElseThrow(() -> new RuntimeException("Payment not found"));

        try {
            // 2. Verify signature
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            boolean isValid = com.razorpay.Utils.verifyPaymentSignature(options, Config.get("RAZORPAY_API_SECRET"));

            if (!isValid) {
                payment.setStatus(Payment.Status.FAILED);
                paymentDao.save(payment);
                throw new RuntimeException("Payment signature verification failed");
            }

            // 3. Fetch payment details from Razorpay
            com.razorpay.Payment razorpayPayment = razorpayClient.payments.fetch(razorpayPaymentId);

            // 4. Update payment record
            payment.setProviderPaymentId(razorpayPaymentId);
            payment.setStatus(Payment.Status.SUCCESS);

            // Update payment mode based on actual payment method
            String method = razorpayPayment.get("method");
            payment.setPaymentMode(mapRazorpayMethod(method));

            paymentDao.save(payment);

            // 5. Update order status
            orderService.confirmOrderPayment(payment.getOrder().getId());

            log.info("Payment successful for order: {}, payment: {}", payment.getOrder().getId(), razorpayPaymentId);

        } catch (RazorpayException e) {
            log.error("Payment verification failed", e);
            payment.setStatus(Payment.Status.FAILED);
            paymentDao.save(payment);
            throw new RuntimeException("Payment verification failed: " + e.getMessage());
        }
    }

    @Transactional
    public void verifyPaymentStatus(UUID orderId) {
        Order order = orderDao.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = paymentDao.findByOrderId(orderId).stream().filter(p -> p.getStatus() == Payment.Status.INITIATED || p.getStatus() == Payment.Status.SUCCESS).findFirst().orElse(null);

        if (payment == null) {
            return;
        }

        try {
            // Fetch payment status from Razorpay
            com.razorpay.Payment razorpayPayment = razorpayClient.payments.fetch(payment.getProviderPaymentId());
            String status = razorpayPayment.get("status");

            if ("captured".equals(status) || "authorized".equals(status)) {
                if (payment.getStatus() != Payment.Status.SUCCESS) {
                    payment.setStatus(Payment.Status.SUCCESS);
                    paymentDao.save(payment);
                    orderService.confirmOrderPayment(orderId);
                }
            } else if ("failed".equals(status)) {
                payment.setStatus(Payment.Status.FAILED);
                paymentDao.save(payment);
            }

        } catch (RazorpayException e) {
            log.error("Failed to verify payment status for order: {}", orderId, e);
        }
    }

    private Payment.PaymentMode mapRazorpayMethod(String method) {
        return switch (method.toLowerCase()) {
            case "upi" -> Payment.PaymentMode.UPI;
            case "card" -> Payment.PaymentMode.CARD;
            case "netbanking" -> Payment.PaymentMode.NET_BANKING;
            default -> Payment.PaymentMode.UPI; // Default
        };
    }

    private void handlePaymentAuthorized(Payment payment, String razorpayPaymentId, String method) {
        // Payment is authorized but not yet captured
        // For auto-capture, this will be followed by payment.captured event
        payment.setProviderPaymentId(razorpayPaymentId);
        payment.setPaymentMode(mapRazorpayMethod(method));
        // Keep status as INITIATED until captured
        paymentDao.save(payment);

        log.info("Payment authorized for order: {}", payment.getOrder().getId());
    }

    private void handlePaymentCaptured(Payment payment, String razorpayPaymentId, String method) {
        // Payment is successfully captured
        payment.setProviderPaymentId(razorpayPaymentId);
        payment.setPaymentMode(mapRazorpayMethod(method));
        payment.setStatus(Payment.Status.SUCCESS);
        payment.setCollectedAt(LocalDateTime.now());
        paymentDao.save(payment);

        // Update order status
        Order order = payment.getOrder();
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderDao.save(order);

        log.info("Payment captured successfully for order: {}, payment: {}", order.getId(), razorpayPaymentId);
    }

    private void handlePaymentFailed(Payment payment, String razorpayPaymentId) {
        payment.setProviderPaymentId(razorpayPaymentId);
        payment.setStatus(Payment.Status.FAILED);
        paymentDao.save(payment);

        // Update order status
        Order order = payment.getOrder();
        order.setPaymentStatus(Order.PaymentStatus.FAILED);
        orderDao.save(order);

        log.warn("Payment failed for order: {}, payment: {}", order.getId(), razorpayPaymentId);
    }

    /**
     * Verify webhook signature to ensure it's from Razorpay
     * Uses Razorpay SDK's built-in verification method
     */
    private boolean verifyWebhookSignature(String webhookBody, String receivedSignature) {
        if (receivedSignature == null || receivedSignature.isEmpty()) {
            log.warn("No signature provided in webhook request");
            return false;
        }

        try {
            // Razorpay SDK's built-in webhook signature verification
            return Utils.verifyWebhookSignature(webhookBody, receivedSignature, Config.get("RAZORPAY_API_SECRET"));
        } catch (RazorpayException e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }
}
