package com.ticketsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String venueName;
    private LocalDateTime eventDate;
    private String status;
    private double totalAmount;
    private LocalDateTime paymentTime;
    private LocalDateTime checkInTime;
    private LocalDateTime createdAt;
    private List<String> seats; // e.g. ["A1", "A2"]
    private String paymentUrl; // Payment Gateway URL
}
