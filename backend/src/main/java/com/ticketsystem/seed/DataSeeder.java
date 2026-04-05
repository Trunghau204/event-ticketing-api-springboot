package com.ticketsystem.seed;

import com.ticketsystem.entity.*;
import com.ticketsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

        private final UserRepository userRepository;
        private final VenueRepository venueRepository;
        private final SeatRepository seatRepository;
        private final EventRepository eventRepository;
        private final EventSeatRepository eventSeatRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) {
                if (userRepository.count() > 0)
                        return; // Already seeded

                // ===== USERS =====
                User admin = User.builder()
                                .username("admin")
                                .email("admin@ticketsystem.com")
                                .password(passwordEncoder.encode("admin123"))
                                .fullName("Administrator")
                                .phone("0123456789")
                                .role(Role.ADMIN)
                                .build();
                userRepository.save(admin);

                User user1 = User.builder()
                                .username("nguyenvana")
                                .email("nguyenvana@gmail.com")
                                .password(passwordEncoder.encode("123456"))
                                .fullName("Nguyen Van A")
                                .phone("0987654321")
                                .role(Role.USER)
                                .build();
                userRepository.save(user1);

                User user2 = User.builder()
                                .username("tranthib")
                                .email("tranthib@gmail.com")
                                .password(passwordEncoder.encode("123456"))
                                .fullName("Tran Thi B")
                                .phone("0912345678")
                                .role(Role.USER)
                                .build();
                userRepository.save(user2);

                // ===== VENUES WITH CUSTOM SEAT LAYOUTS =====

                // Venue 1: Nhà hát Lớn - 8 rows, various seat counts
                Venue venue1 = createVenue("Nha hat Lon Ha Noi", "1 Trang Tien, Hoan Kiem, Ha Noi",
                                new String[][] {
                                                { "A", "10", "VIP" },
                                                { "B", "10", "VIP" },
                                                { "C", "12", "STANDARD" },
                                                { "D", "12", "STANDARD" },
                                                { "E", "12", "STANDARD" },
                                                { "F", "14", "ECONOMY" },
                                                { "G", "14", "ECONOMY" },
                                                { "H", "14", "ECONOMY" }
                                });

                // Venue 2: Nhà thi đấu - 10 rows
                Venue venue2 = createVenue("Nha Thi Dau Phu Tho", "1 Lu Gia, Quan 11, TP.HCM",
                                new String[][] {
                                                { "A", "12", "VIP" },
                                                { "B", "12", "VIP" },
                                                { "C", "14", "VIP" },
                                                { "D", "15", "STANDARD" },
                                                { "E", "15", "STANDARD" },
                                                { "F", "15", "STANDARD" },
                                                { "G", "15", "STANDARD" },
                                                { "H", "18", "ECONOMY" },
                                                { "I", "18", "ECONOMY" },
                                                { "J", "18", "ECONOMY" }
                                });

                // Venue 3: Sân vận động - 15 rows, big
                Venue venue3 = createVenue("San Van Dong My Dinh", "Le Duc Tho, Nam Tu Liem, Ha Noi",
                                new String[][] {
                                                { "A", "15", "VIP" },
                                                { "B", "15", "VIP" },
                                                { "C", "15", "VIP" },
                                                { "D", "18", "STANDARD" },
                                                { "E", "18", "STANDARD" },
                                                { "F", "18", "STANDARD" },
                                                { "G", "20", "STANDARD" },
                                                { "H", "20", "STANDARD" },
                                                { "I", "20", "STANDARD" },
                                                { "J", "22", "ECONOMY" },
                                                { "K", "22", "ECONOMY" },
                                                { "L", "22", "ECONOMY" },
                                                { "M", "22", "ECONOMY" },
                                                { "N", "22", "ECONOMY" },
                                                { "O", "22", "ECONOMY" }
                                });

                // ===== EVENTS WITH PER-TYPE PRICING =====

                // Event 1: Concert - VIP 750K, Standard 500K, Economy 300K
                createEvent("Live Concert Son Tung M-TP",
                                "Dem nhac live cua Son Tung M-TP voi nhieu ca khuc hit. Chuong trinh bao gom: phan giao luu, mini game, va trinh dien dac biet.",
                                venue1, LocalDateTime.of(2026, 4, 15, 19, 30),
                                750000, 500000, 300000,
                                "https://picsum.photos/seed/concert1/800/400");

                // Event 2: Tech Conference - VIP 500K, Standard 300K, Economy 150K
                createEvent("Tech Conference 2026",
                                "Hoi nghi cong nghe hang dau Viet Nam. Cac dien gia tu Google, Meta se chia se ve AI, Cloud, va Blockchain.",
                                venue2, LocalDateTime.of(2026, 5, 20, 8, 0),
                                500000, 300000, 150000,
                                "https://picsum.photos/seed/tech1/800/400");

                // Event 3: Festival - VIP 1.2M, Standard 800K, Economy 500K
                createEvent("Festival Am Nhac Quoc Te",
                                "Festival am nhac voi su tham gia cua cac nghe sy quoc te va trong nuoc. 2 ngay, nhieu san khau, hon 30 nghe sy trinh dien.",
                                venue3, LocalDateTime.of(2026, 6, 10, 17, 0),
                                1200000, 800000, 500000,
                                "https://picsum.photos/seed/festival1/800/400");

                // Event 4: Comedy - VIP 350K, Standard 200K, Economy 100K
                createEvent("Stand-up Comedy Night",
                                "Dem hai doc thoai voi cac comedian noi tieng: Xuan Bac, Truong Giang, Tran Thanh.",
                                venue1, LocalDateTime.of(2026, 3, 25, 20, 0),
                                350000, 200000, 100000,
                                "https://picsum.photos/seed/comedy1/800/400");

                // Event 5: Football - VIP 1.5M, Standard 1M, Economy 600K
                createEvent("Tran Chung Ket AFF Cup 2026",
                                "Tran chung ket luot ve AFF Cup 2026. Viet Nam vs Thai Lan. Hay den va co vu cho doi tuyen Viet Nam!",
                                venue3, LocalDateTime.of(2026, 7, 1, 19, 0),
                                1500000, 1000000, 600000,
                                "https://picsum.photos/seed/football1/800/400");

                System.out.println("=== DATA SEEDER: Da tao du lieu mau thanh cong! ===");
                System.out.println("Admin: admin / admin123");
                System.out.println("User 1: nguyenvana / 123456");
                System.out.println("User 2: tranthib / 123456");
                System.out.println("Events: 5 su kien | Venues: 3 dia diem");
        }

        private Venue createVenue(String name, String address, String[][] rowConfigs) {
                int totalRows = rowConfigs.length;
                int maxCols = 0;
                for (String[] row : rowConfigs) {
                        maxCols = Math.max(maxCols, Integer.parseInt(row[1]));
                }

                Venue venue = Venue.builder()
                                .name(name)
                                .address(address)
                                .totalRows(totalRows)
                                .totalColumns(maxCols)
                                .build();
                venue = venueRepository.save(venue);

                // Generate seats from config
                List<Seat> seats = new ArrayList<>();
                for (String[] row : rowConfigs) {
                        String label = row[0];
                        int seatCount = Integer.parseInt(row[1]);
                        String seatType = row[2];
                        for (int col = 1; col <= seatCount; col++) {
                                seats.add(Seat.builder()
                                                .venue(venue)
                                                .rowLabel(label)
                                                .colNumber(col)
                                                .seatType(seatType)
                                                .build());
                        }
                }
                seatRepository.saveAll(seats);
                return venue;
        }

        private void createEvent(String title, String description, Venue venue, LocalDateTime eventDate,
                        double priceVip, double priceStandard, double priceEconomy, String imageUrl) {
                Event event = Event.builder()
                                .title(title)
                                .description(description)
                                .venue(venue)
                                .eventDate(eventDate)
                                .price(priceStandard) // Display price = standard
                                .imageUrl(imageUrl)
                                .status("UPCOMING")
                                .build();
                event = eventRepository.save(event);

                // Clone seats with per-type pricing
                List<Seat> seats = seatRepository.findByVenueId(venue.getId());
                List<EventSeat> eventSeats = new ArrayList<>();
                for (Seat seat : seats) {
                        double price = switch (seat.getSeatType()) {
                                case "VIP" -> priceVip;
                                case "STANDARD" -> priceStandard;
                                case "ECONOMY" -> priceEconomy;
                                default -> priceStandard;
                        };
                        eventSeats.add(EventSeat.builder()
                                        .event(event)
                                        .seat(seat)
                                        .status(SeatStatus.AVAILABLE)
                                        .price(price)
                                        .build());
                }
                eventSeatRepository.saveAll(eventSeats);
        }
}
