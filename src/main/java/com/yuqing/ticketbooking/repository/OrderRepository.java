package com.yuqing.ticketbooking.repository;

import com.yuqing.ticketbooking.entity.TicketOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<TicketOrder, Long> {
}
