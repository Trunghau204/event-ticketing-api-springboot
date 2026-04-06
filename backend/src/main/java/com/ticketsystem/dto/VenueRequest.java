package com.ticketsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class VenueRequest {
    @NotBlank
    private String name;

    private String address;

    // Legacy fields (kept for backward compatibility)
    private int totalRows;
    private int totalColumns;

    // New: custom row configuration
    private List<SeatRowConfig> seatRows;
}
