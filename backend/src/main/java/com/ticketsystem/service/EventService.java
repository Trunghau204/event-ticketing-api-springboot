package com.ticketsystem.service;

import com.ticketsystem.dto.EventRequest;
import com.ticketsystem.dto.EventResponse;
import com.ticketsystem.dto.EventSeatResponse;
import com.ticketsystem.entity.*;
import com.ticketsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final EventSeatRepository eventSeatRepository;

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents() {
        return eventRepository.findByStatus("UPCOMING").stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại!"));
        return toResponse(event);
    }

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        // Validate event date is in the future
        LocalDateTime eventDate = LocalDateTime.parse(request.getEventDate());
        if (eventDate.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Ngày sự kiện phải sau thời điểm hiện tại!");
        }

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new RuntimeException("Venue không tồn tại!"));

        // Use priceStandard as the base display price
        double displayPrice = request.getPriceStandard() > 0
                ? request.getPriceStandard()
                : request.getPrice();

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .venue(venue)
                .eventDate(eventDate)
                .price(displayPrice)
                .imageUrl(request.getImageUrl())
                .status("UPCOMING")
                .build();

        event = eventRepository.save(event);

        // Clone Seat → EventSeat with per-type pricing
        cloneSeatsToEventSeats(event, venue, request);

        return toResponse(event);
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại!"));

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(LocalDateTime.parse(request.getEventDate()));

        double displayPrice = request.getPriceStandard() > 0
                ? request.getPriceStandard()
                : request.getPrice();
        event.setPrice(displayPrice);
        event.setImageUrl(request.getImageUrl());

        if (!event.getVenue().getId().equals(request.getVenueId())) {
            Venue newVenue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new RuntimeException("Venue không tồn tại!"));
            event.setVenue(newVenue);

            // Re-clone seats if venue changed
            eventSeatRepository.deleteAll(eventSeatRepository.findByEventId(id));
            cloneSeatsToEventSeats(event, newVenue, request);
        } else {
            // Update prices on existing seats
            updateEventSeatPrices(event, request);
        }

        event = eventRepository.save(event);
        return toResponse(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Sự kiện không tồn tại!");
        }
        eventRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<EventSeatResponse> getEventSeats(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại!"));

        // Auto-sync: check if venue has new seats not yet linked to this event
        List<Seat> venueSeats = seatRepository.findByVenueId(event.getVenue().getId());
        List<EventSeat> existingEventSeats = eventSeatRepository.findByEventId(eventId);

        java.util.Set<Long> linkedSeatIds = existingEventSeats.stream()
                .map(es -> es.getSeat().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<EventSeat> newEventSeats = new ArrayList<>();
        for (Seat seat : venueSeats) {
            if (!linkedSeatIds.contains(seat.getId())) {
                newEventSeats.add(EventSeat.builder()
                        .event(event)
                        .seat(seat)
                        .status(SeatStatus.AVAILABLE)
                        .price(event.getPrice())
                        .build());
            }
        }

        if (!newEventSeats.isEmpty()) {
            eventSeatRepository.saveAll(newEventSeats);
            existingEventSeats.addAll(newEventSeats);
        }

        return existingEventSeats.stream().map(this::toSeatResponse).toList();
    }

    private void cloneSeatsToEventSeats(Event event, Venue venue, EventRequest request) {
        List<Seat> seats = seatRepository.findByVenueId(venue.getId());
        List<EventSeat> eventSeats = new ArrayList<>();

        for (Seat seat : seats) {
            double seatPrice = getPriceForSeatType(seat.getSeatType(), request);
            EventSeat eventSeat = EventSeat.builder()
                    .event(event)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .price(seatPrice)
                    .build();
            eventSeats.add(eventSeat);
        }

        eventSeatRepository.saveAll(eventSeats);
    }

    private void updateEventSeatPrices(Event event, EventRequest request) {
        List<EventSeat> eventSeats = eventSeatRepository.findByEventId(event.getId());
        for (EventSeat es : eventSeats) {
            double newPrice = getPriceForSeatType(es.getSeat().getSeatType(), request);
            es.setPrice(newPrice);
        }
        eventSeatRepository.saveAll(eventSeats);
    }

    private double getPriceForSeatType(String seatType, EventRequest request) {
        if (request.getPriceVip() > 0 || request.getPriceStandard() > 0 || request.getPriceEconomy() > 0) {
            return switch (seatType) {
                case "VIP" -> request.getPriceVip();
                case "STANDARD" -> request.getPriceStandard();
                case "ECONOMY" -> request.getPriceEconomy();
                default -> request.getPriceStandard();
            };
        }
        // Fallback to single price
        return request.getPrice();
    }

    private EventResponse toResponse(Event event) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setTitle(event.getTitle());
        response.setDescription(event.getDescription());
        response.setVenueId(event.getVenue().getId());
        response.setVenueName(event.getVenue().getName());
        response.setVenueAddress(event.getVenue().getAddress());
        response.setEventDate(event.getEventDate());
        response.setPrice(event.getPrice());
        response.setImageUrl(event.getImageUrl());

        // Compute status dynamically based on current time
        String status = event.getStatus();
        if (event.getEventDate() != null && !"CANCELLED".equals(status)) {
            if (event.getEventDate().isBefore(LocalDateTime.now())) {
                status = "COMPLETED";
            } else {
                status = "UPCOMING";
            }
        }
        response.setStatus(status);

        long totalSeats = eventSeatRepository.findByEventId(event.getId()).size();
        long availableSeats = eventSeatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.AVAILABLE);
        response.setTotalSeats((int) totalSeats);
        response.setAvailableSeats((int) availableSeats);

        return response;
    }

    private EventSeatResponse toSeatResponse(EventSeat eventSeat) {
        EventSeatResponse response = new EventSeatResponse();
        response.setId(eventSeat.getId());
        response.setRowLabel(eventSeat.getSeat().getRowLabel());
        response.setColNumber(eventSeat.getSeat().getColNumber());
        response.setSeatType(eventSeat.getSeat().getSeatType());
        response.setStatus(eventSeat.getStatus().name());
        response.setPrice(eventSeat.getPrice());
        return response;
    }
}
