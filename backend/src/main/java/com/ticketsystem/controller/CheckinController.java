package com.ticketsystem.controller;

import com.ticketsystem.dto.CheckinRequest;
import com.ticketsystem.dto.CheckinResponse;
import com.ticketsystem.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping("/scan")
    public ResponseEntity<CheckinResponse> scanQr(@RequestBody CheckinRequest request) {
        return ResponseEntity.ok(checkinService.processCheckin(request.getQrContent()));
    }
}
