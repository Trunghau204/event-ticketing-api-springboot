package com.ticketsystem.service;

import com.ticketsystem.dto.CheckinResponse;
import com.ticketsystem.entity.Order;
import com.ticketsystem.entity.OrderStatus;
import com.ticketsystem.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private final OrderRepository orderRepository;

    @Transactional
    public CheckinResponse processCheckin(String qrContent) {
        try {
            // Parse QR content: orderId|eventId|userId|seats|checksum
            String[] parts = qrContent.split("\\|");
            if (parts.length < 4) {
                return new CheckinResponse(false, "QR Code không hợp lệ!", null, null, null);
            }

            Long orderId = Long.parseLong(parts[0]);

            Order order = orderRepository.findById(orderId)
                    .orElse(null);

            if (order == null) {
                return new CheckinResponse(false, "Đơn hàng không tồn tại!", null, null, null);
            }

            if (order.getStatus() != OrderStatus.PAID) {
                return new CheckinResponse(false, "Đơn hàng chưa thanh toán!", null, null, null);
            }

            if (order.getCheckInTime() != null) {
                return new CheckinResponse(false,
                        "Vé đã được check-in trước đó lúc: " + order.getCheckInTime(),
                        order.getEvent().getTitle(),
                        order.getUser().getFullName(),
                        null);
            }

            // Update check-in time
            order.setCheckInTime(LocalDateTime.now());
            orderRepository.save(order);

            String seats = order.getOrderItems().stream()
                    .map(item -> item.getEventSeat().getSeat().getRowLabel() +
                            item.getEventSeat().getSeat().getColNumber())
                    .collect(Collectors.joining(", "));

            return new CheckinResponse(true,
                    "Check-in thành công!",
                    order.getEvent().getTitle(),
                    order.getUser().getFullName(),
                    seats);

        } catch (Exception e) {
            return new CheckinResponse(false, "Lỗi xử lý QR: " + e.getMessage(), null, null, null);
        }
    }
}
