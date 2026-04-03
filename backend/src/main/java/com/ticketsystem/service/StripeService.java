package com.ticketsystem.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public String createCheckoutSession(Long orderId, double amount, String eventTitle, String seats)
            throws StripeException {
        // Stripe expects amount in smallest currency unit (VND has no decimal)
        long amountInSmallest = (long) amount;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}&orderId=" + orderId)
                .setCancelUrl(cancelUrl + "?orderId=" + orderId)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("vnd")
                                                .setUnitAmount(amountInSmallest)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Vé: " + eventTitle)
                                                                .setDescription("Ghế: " + seats)
                                                                .build())
                                                .build())
                                .build())
                .putMetadata("orderId", orderId.toString())
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    public Session getSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}
