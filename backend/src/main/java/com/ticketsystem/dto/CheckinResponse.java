package com.ticketsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckinResponse {
    private boolean valid;
    private String message;
    private String eventTitle;
    private String userName;
    private String seats;
}
