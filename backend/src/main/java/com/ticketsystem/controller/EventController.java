package com.ticketsystem.controller;

import com.ticketsystem.dto.EventRequest;
import com.ticketsystem.dto.EventResponse;
import com.ticketsystem.dto.EventSeatResponse;
import com.ticketsystem.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // ===== PUBLIC ENDPOINTS =====

    @GetMapping("/api/events")
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/api/events/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/api/events/{id}/seats")
    public ResponseEntity<List<EventSeatResponse>> getEventSeats(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventSeats(id));
    }

    // ===== ADMIN ENDPOINTS =====

    @PostMapping("/api/admin/events")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.createEvent(request));
    }

    @PutMapping("/api/admin/events/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @DeleteMapping("/api/admin/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
