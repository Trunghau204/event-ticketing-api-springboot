package com.ticketsystem.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ReserveRequest {
    @NotNull
    private Long eventId;

    @NotEmpty
    private List<Long> eventSeatIds;
}
