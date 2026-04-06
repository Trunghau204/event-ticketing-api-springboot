package com.ticketsystem.service;

import com.ticketsystem.dto.SeatRowConfig;
import com.ticketsystem.dto.VenueRequest;
import com.ticketsystem.dto.VenueResponse;
import com.ticketsystem.entity.Event;
import com.ticketsystem.entity.EventSeat;
import com.ticketsystem.entity.Seat;
import com.ticketsystem.entity.SeatStatus;
import com.ticketsystem.entity.Venue;
import com.ticketsystem.repository.EventRepository;
import com.ticketsystem.repository.EventSeatRepository;
import com.ticketsystem.repository.SeatRepository;
import com.ticketsystem.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;

    @Transactional(readOnly = true)
    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VenueResponse getVenueById(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venue không tồn tại!"));
        return toResponse(venue);
    }

    @Transactional
    public VenueResponse createVenue(VenueRequest request) {
        Venue venue = Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .build();

        // Calculate totalRows and totalColumns from seatRows
        if (request.getSeatRows() != null && !request.getSeatRows().isEmpty()) {
            venue.setTotalRows(request.getSeatRows().size());
            int maxCols = request.getSeatRows().stream()
                    .mapToInt(SeatRowConfig::getSeatCount).max().orElse(0);
            venue.setTotalColumns(maxCols);
        } else {
            venue.setTotalRows(request.getTotalRows());
            venue.setTotalColumns(request.getTotalColumns());
        }

        venue = venueRepository.save(venue);

        // Generate seats
        if (request.getSeatRows() != null && !request.getSeatRows().isEmpty()) {
            generateSeatsFromConfig(venue, request.getSeatRows());
        } else {
            generateSeatsDefault(venue);
        }

        return toResponse(venue);
    }

    @Transactional
    public VenueResponse updateVenue(Long id, VenueRequest request) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venue không tồn tại!"));

        venue.setName(request.getName());
        venue.setAddress(request.getAddress());

        if (request.getSeatRows() != null && !request.getSeatRows().isEmpty()) {
            venue.setTotalRows(request.getSeatRows().size());
            int maxCols = request.getSeatRows().stream()
                    .mapToInt(SeatRowConfig::getSeatCount).max().orElse(0);
            venue.setTotalColumns(maxCols);

            // Get existing seats grouped by row
            List<Seat> existingSeats = seatRepository.findByVenueId(id);
            java.util.Map<String, List<Seat>> seatsByRow = existingSeats.stream()
                    .collect(java.util.stream.Collectors.groupingBy(Seat::getRowLabel));

            // Collect requested row labels
            java.util.Set<String> requestedLabels = request.getSeatRows().stream()
                    .map(SeatRowConfig::getLabel)
                    .collect(java.util.stream.Collectors.toSet());

            // 1. DELETE seats for rows that are no longer in the config
            List<Seat> seatsToDelete = new ArrayList<>();
            for (var entry : seatsByRow.entrySet()) {
                if (!requestedLabels.contains(entry.getKey())) {
                    seatsToDelete.addAll(entry.getValue());
                }
            }
            if (!seatsToDelete.isEmpty()) {
                List<Long> seatIdsToDelete = seatsToDelete.stream().map(Seat::getId).toList();
                // Delete event_seats first (FK constraint)
                List<EventSeat> eventSeatsToDelete = eventSeatRepository.findBySeatIdIn(seatIdsToDelete);
                eventSeatRepository.deleteAll(eventSeatsToDelete);
                seatRepository.deleteAll(seatsToDelete);
            }

            // 2. ADD/UPDATE seats for each requested row
            List<Seat> allNewSeats = new ArrayList<>();
            for (SeatRowConfig config : request.getSeatRows()) {
                List<Seat> rowSeats = seatsByRow.getOrDefault(config.getLabel(), List.of());
                int existingCount = rowSeats.size();
                int wantedCount = config.getSeatCount();

                // Add missing seats (new row or extra columns)
                for (int col = existingCount + 1; col <= wantedCount; col++) {
                    allNewSeats.add(Seat.builder()
                            .venue(venue)
                            .rowLabel(config.getLabel())
                            .colNumber(col)
                            .seatType(config.getSeatType())
                            .build());
                }

                // Delete extra seats if wantedCount < existingCount
                if (wantedCount < existingCount) {
                    List<Seat> extraSeats = rowSeats.stream()
                            .filter(s -> s.getColNumber() > wantedCount)
                            .toList();
                    if (!extraSeats.isEmpty()) {
                        List<Long> extraIds = extraSeats.stream().map(Seat::getId).toList();
                        List<EventSeat> extraEventSeats = eventSeatRepository.findBySeatIdIn(extraIds);
                        eventSeatRepository.deleteAll(extraEventSeats);
                        seatRepository.deleteAll(extraSeats);
                    }
                }

                // Update seat type for remaining existing seats
                for (Seat seat : rowSeats) {
                    if (seat.getColNumber() <= wantedCount && !seat.getSeatType().equals(config.getSeatType())) {
                        seat.setSeatType(config.getSeatType());
                        seatRepository.save(seat);
                    }
                }
            }

            // 3. SAVE new seats and sync to events
            if (!allNewSeats.isEmpty()) {
                allNewSeats = seatRepository.saveAll(allNewSeats);

                List<Event> events = eventRepository.findByVenueId(id);
                if (!events.isEmpty()) {
                    List<EventSeat> newEventSeats = new ArrayList<>();
                    for (Event event : events) {
                        for (Seat newSeat : allNewSeats) {
                            newEventSeats.add(EventSeat.builder()
                                    .event(event)
                                    .seat(newSeat)
                                    .status(SeatStatus.AVAILABLE)
                                    .price(event.getPrice())
                                    .build());
                        }
                    }
                    eventSeatRepository.saveAll(newEventSeats);
                }
            }
        } else {
            venue.setTotalRows(request.getTotalRows());
            venue.setTotalColumns(request.getTotalColumns());
        }

        venue = venueRepository.save(venue);
        return toResponse(venue);
    }

    @Transactional
    public void deleteVenue(Long id) {
        if (!venueRepository.existsById(id)) {
            throw new RuntimeException("Venue không tồn tại!");
        }
        venueRepository.deleteById(id);
    }

    private void generateSeatsFromConfig(Venue venue, List<SeatRowConfig> rowConfigs) {
        List<Seat> seats = new ArrayList<>();
        for (SeatRowConfig config : rowConfigs) {
            for (int col = 1; col <= config.getSeatCount(); col++) {
                seats.add(Seat.builder()
                        .venue(venue)
                        .rowLabel(config.getLabel())
                        .colNumber(col)
                        .seatType(config.getSeatType())
                        .build());
            }
        }
        seatRepository.saveAll(seats);
    }

    private void generateSeatsDefault(Venue venue) {
        List<Seat> seats = new ArrayList<>();
        for (int row = 0; row < venue.getTotalRows(); row++) {
            String rowLabel = String.valueOf((char) ('A' + row));
            String seatType = row < 2 ? "VIP" : (row < venue.getTotalRows() / 2 ? "STANDARD" : "ECONOMY");
            for (int col = 1; col <= venue.getTotalColumns(); col++) {
                seats.add(Seat.builder()
                        .venue(venue)
                        .rowLabel(rowLabel)
                        .colNumber(col)
                        .seatType(seatType)
                        .build());
            }
        }
        seatRepository.saveAll(seats);
    }

    private VenueResponse toResponse(Venue venue) {
        VenueResponse response = new VenueResponse();
        response.setId(venue.getId());
        response.setName(venue.getName());
        response.setAddress(venue.getAddress());
        response.setTotalRows(venue.getTotalRows());
        response.setTotalColumns(venue.getTotalColumns());

        // Count actual seats from DB
        long actualSeatCount = seatRepository.findByVenueId(venue.getId()).size();
        response.setTotalSeats((int) actualSeatCount);

        return response;
    }
}
