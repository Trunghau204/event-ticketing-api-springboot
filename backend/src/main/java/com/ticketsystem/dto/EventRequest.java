package com.ticketsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class EventRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long venueId;

    @NotBlank
    private String eventDate; // ISO format: 2025-12-31T20:00:00

    // Keep legacy single price for backward compat
    private double price;

    // New: per-type pricing
    @Positive
    private double priceVip;

    @Positive
    private double priceStandard;

    @Positive
    private double priceEconomy;

    private String imageUrl;
}
