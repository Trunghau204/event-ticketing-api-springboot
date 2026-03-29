package com.ticketsystem.dto;

import lombok.Data;

@Data
public class EventSeatResponse {
    private Long id;
    private String rowLabel;
    private int colNumber;
    private String seatType;
    private String status;
    private double price;
    private boolean checkedIn;
    private String bookedBy;
    private String bookedEmail;
    private String bookedPhone;
}
