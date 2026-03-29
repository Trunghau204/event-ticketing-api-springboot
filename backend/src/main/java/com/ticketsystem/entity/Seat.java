package com.ticketsystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false)
    private String rowLabel; // A, B, C, ...

    @Column(nullable = false)
    private int colNumber; // 1, 2, 3, ...

    private String seatType; // VIP, STANDARD, ECONOMY
}
