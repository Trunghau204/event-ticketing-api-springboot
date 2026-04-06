package com.ticketsystem.repository;

import com.ticketsystem.entity.Order;
import com.ticketsystem.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    List<Order> findByEventId(Long eventId);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime time);
}
