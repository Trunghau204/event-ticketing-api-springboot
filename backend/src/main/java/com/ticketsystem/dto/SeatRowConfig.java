package com.ticketsystem.dto;

import lombok.Data;

@Data
public class SeatRowConfig {
    private String label; // A, B, C, ...
    private int seatCount; // number of seats in this row
    private String seatType; // VIP, STANDARD, ECONOMY
}
