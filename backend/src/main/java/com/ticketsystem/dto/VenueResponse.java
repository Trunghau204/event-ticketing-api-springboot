package com.ticketsystem.dto;

import lombok.Data;

@Data
public class VenueResponse {
    private Long id;
    private String name;
    private String address;
    private int totalRows;
    private int totalColumns;
    private int totalSeats;
}
