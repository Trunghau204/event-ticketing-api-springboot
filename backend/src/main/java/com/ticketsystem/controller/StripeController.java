package com.ticketsystem.controller;

import com.stripe.model.checkout.Session;
import com.ticketsystem.dto.OrderResponse;
import com.ticketsystem.service.OrderService;
import com.ticketsystem.service.StripeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripeService stripeService;
    private final OrderService orderService;

    /**
     * Verify Stripe payment after checkout success redirect
     */
    @GetMapping("/verify")
    public ResponseEntity<OrderResponse> verifyPayment(@RequestParam String sessionId, @RequestParam Long orderId) {
        try {
            Session session = stripeService.getSession(sessionId);

            if ("complete".equals(session.getStatus()) || "paid".equals(session.getPaymentStatus())) {
                OrderResponse response = orderService.processPayment(orderId, "STRIPE_" + session.getPaymentIntent());
                return ResponseEntity.ok(response);
            } else {
                throw new RuntimeException("Thanh toán chưa hoàn tất! Status: " + session.getStatus());
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xác minh thanh toán Stripe: " + e.getMessage());
        }
    }
}
