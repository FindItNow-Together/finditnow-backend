package com.finditnow.orderservice.controllers;

import com.finditnow.orderservice.dtos.InitiatePaymentRequest;
import com.finditnow.orderservice.dtos.PaymentInitiationResponse;
import com.finditnow.orderservice.services.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @RequestBody InitiatePaymentRequest request
    ) {
        PaymentInitiationResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Razorpay webhook endpoint for payment events
     * This is called by Razorpay when payment status changes
     * Configured at razorpay dashboard
     */
    @PostMapping("/webhook/razorpay")
    public ResponseEntity<Map<String, String>> handleRazorpayWebhook(
            @RequestBody String webhookBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        try {
            log.info("Received Razorpay webhook");

            paymentService.handleRazorpayWebhook(webhookBody, signature);

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            // Return 200 even on error to prevent Razorpay from retrying
            // Log the error for manual investigation
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> handlePaymentCallback(
            @RequestBody Map<String, String> payload
    ) {
        String razorpayOrderId = payload.get("razorpay_order_id");
        String razorpayPaymentId = payload.get("razorpay_payment_id");
        String razorpaySignature = payload.get("razorpay_signature");

        try {
            paymentService.handlePaymentCallback(razorpayOrderId, razorpayPaymentId, razorpaySignature);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Payment callback failed", e);
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    @GetMapping("/verify/{orderId}")
    public ResponseEntity<Void> verifyPayment(@PathVariable UUID orderId) {
        paymentService.verifyPaymentStatus(orderId);
        return ResponseEntity.ok().build();
    }
}