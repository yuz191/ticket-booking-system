package com.yuqing.ticket_booking.repository;

import com.yuqing.ticket_booking.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
}
