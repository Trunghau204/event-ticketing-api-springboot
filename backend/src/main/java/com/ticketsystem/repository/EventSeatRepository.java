package com.ticketsystem.repository;

import com.ticketsystem.entity.EventSeat;
import com.ticketsystem.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventSeatRepository extends JpaRepository<EventSeat, Long> {

    List<EventSeat> findByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT es FROM EventSeat es WHERE es.id = :id")
    Optional<EventSeat> findByIdWithLock(@Param("id") Long id);

    List<EventSeat> findByEventIdAndStatus(Long eventId, SeatStatus status);

    long countByEventIdAndStatus(Long eventId, SeatStatus status);

    List<EventSeat> findBySeatId(Long seatId);

    List<EventSeat> findBySeatIdIn(List<Long> seatIds);
}
