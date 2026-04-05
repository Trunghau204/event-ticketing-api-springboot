package com.ticketsystem.controller;

import com.ticketsystem.dto.DashboardStatsResponse;
import com.ticketsystem.dto.DashboardStatsResponse.EventRevenueItem;
import com.ticketsystem.dto.DashboardStatsResponse.RecentOrderItem;
import com.ticketsystem.dto.EventSeatResponse;
import com.ticketsystem.entity.*;
import com.ticketsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

        private final EventRepository eventRepository;
        private final OrderRepository orderRepository;
        private final EventSeatRepository eventSeatRepository;
        private final UserRepository userRepository;

        @GetMapping("/stats")
        public ResponseEntity<DashboardStatsResponse> getStats() {
                long totalEvents = eventRepository.count();
                long totalUsers = userRepository.count();
                List<Order> allOrders = orderRepository.findAll();
                List<Event> allEvents = eventRepository.findAll();

                List<Order> paidOrders = allOrders.stream()
                                .filter(o -> o.getStatus() == OrderStatus.PAID || o.getCheckInTime() != null)
                                .toList();

                long totalOrders = paidOrders.size();

                double totalRevenue = paidOrders.stream()
                                .mapToDouble(Order::getTotalAmount)
                                .sum();

                long totalCheckedIn = allOrders.stream()
                                .filter(o -> o.getCheckInTime() != null)
                                .count();

                long totalTicketsSold = paidOrders.stream()
                                .mapToLong(o -> o.getOrderItems() != null ? o.getOrderItems().size() : 0)
                                .sum();

                // Per-event revenue data for charts
                List<EventRevenueItem> revenueByEvent = new ArrayList<>();
                for (Event event : allEvents) {
                        List<Order> eventOrders = allOrders.stream()
                                        .filter(o -> o.getEvent().getId().equals(event.getId()))
                                        .filter(o -> o.getStatus() == OrderStatus.PAID || o.getCheckInTime() != null)
                                        .toList();

                        double revenue = eventOrders.stream().mapToDouble(Order::getTotalAmount).sum();
                        long tickets = eventOrders.stream()
                                        .mapToLong(o -> o.getOrderItems() != null ? o.getOrderItems().size() : 0)
                                        .sum();
                        long checked = eventOrders.stream().filter(o -> o.getCheckInTime() != null).count();
                        long totalSeats = eventSeatRepository.findByEventId(event.getId()).size();

                        revenueByEvent.add(new EventRevenueItem(
                                        event.getTitle(), revenue, tickets, checked, totalSeats));
                }

                // Recent orders (latest 10)
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                List<RecentOrderItem> recentOrders = allOrders.stream()
                                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                                .limit(10)
                                .map(o -> new RecentOrderItem(
                                                o.getId(),
                                                o.getUser().getFullName(),
                                                o.getEvent().getTitle(),
                                                o.getTotalAmount(),
                                                o.getCheckInTime() != null ? "CHECKED_IN" : o.getStatus().name(),
                                                o.getCreatedAt().format(fmt),
                                                o.getOrderItems() != null ? o.getOrderItems().size() : 0))
                                .toList();

                DashboardStatsResponse response = new DashboardStatsResponse();
                response.setTotalEvents(totalEvents);
                response.setTotalOrders(totalOrders);
                response.setTotalRevenue(totalRevenue);
                response.setTotalCheckedIn(totalCheckedIn);
                response.setTotalTicketsSold(totalTicketsSold);
                response.setTotalUsers(totalUsers);
                response.setRevenueByEvent(revenueByEvent);
                response.setRecentOrders(recentOrders);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/events/{eventId}/checkin-seats")
        public ResponseEntity<List<EventSeatResponse>> getCheckinSeats(@PathVariable Long eventId) {
                List<EventSeat> eventSeats = eventSeatRepository.findByEventId(eventId);
                List<Order> allEventOrders = orderRepository.findByEventId(eventId);

                Map<Long, Order> seatOrderMap = new HashMap<>();
                for (Order o : allEventOrders) {
                        if (o.getStatus() == OrderStatus.PAID || o.getCheckInTime() != null) {
                                for (OrderItem item : o.getOrderItems()) {
                                        seatOrderMap.put(item.getEventSeat().getId(), o);
                                }
                        }
                }

                List<EventSeatResponse> responses = new ArrayList<>();
                for (EventSeat es : eventSeats) {
                        EventSeatResponse r = new EventSeatResponse();
                        r.setId(es.getId());
                        r.setRowLabel(es.getSeat().getRowLabel());
                        r.setColNumber(es.getSeat().getColNumber());
                        r.setSeatType(es.getSeat().getSeatType());
                        r.setStatus(es.getStatus().name());
                        r.setPrice(es.getPrice());

                        Order order = seatOrderMap.get(es.getId());
                        if (order != null && order.getUser() != null) {
                                r.setCheckedIn(order.getCheckInTime() != null);
                                r.setBookedBy(order.getUser().getFullName() != null
                                                && !order.getUser().getFullName().trim().isEmpty()
                                                                ? order.getUser().getFullName()
                                                                : order.getUser().getUsername());
                                r.setBookedEmail(order.getUser().getEmail());
                                r.setBookedPhone(order.getUser().getPhone());
                        } else {
                                r.setCheckedIn(false);
                        }

                        responses.add(r);
                }

                return ResponseEntity.ok(responses);
        }
}
