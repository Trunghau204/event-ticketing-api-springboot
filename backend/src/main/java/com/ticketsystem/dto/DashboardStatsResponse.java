package com.ticketsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponse {
    private long totalEvents;
    private long totalOrders;
    private double totalRevenue;
    private long totalCheckedIn;
    private long totalTicketsSold;
    private long totalUsers;

    // Chart data: event title -> revenue
    private List<EventRevenueItem> revenueByEvent;

    // Recent orders
    private List<RecentOrderItem> recentOrders;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EventRevenueItem {
        private String eventTitle;
        private double revenue;
        private long ticketsSold;
        private long checkedIn;
        private long totalSeats;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecentOrderItem {
        private Long orderId;
        private String userName;
        private String eventTitle;
        private double amount;
        private String status;
        private String createdAt;
        private int seatCount;
    }
}
