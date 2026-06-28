package com.yuqing.ticket_booking.repository;

import com.yuqing.ticket_booking.entity.TicketOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<TicketOrder, Long> {
}
