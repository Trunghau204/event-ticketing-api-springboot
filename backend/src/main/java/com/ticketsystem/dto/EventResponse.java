package com.ticketsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private Long venueId;
    private String venueName;
    private String venueAddress;
    private LocalDateTime eventDate;
    private double price;
    private String imageUrl;
    private String status;
    private int totalSeats;
    private int availableSeats;
}
