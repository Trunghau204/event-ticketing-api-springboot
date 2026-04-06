package com.ticketsystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private boolean isEnabled;
}
