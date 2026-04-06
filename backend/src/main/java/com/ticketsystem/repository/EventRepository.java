package com.ticketsystem.repository;

import com.ticketsystem.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStatus(String status);

    List<Event> findByVenueId(Long venueId);
}
