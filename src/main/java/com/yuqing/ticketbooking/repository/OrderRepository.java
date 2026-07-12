package com.yuqing.ticketbooking.repository;

import com.yuqing.ticketbooking.entity.OrderStatus;
import com.yuqing.ticketbooking.entity.TicketOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<TicketOrder, Long> {

    List<TicketOrder> findByStatusAndCreatedAtBefore(
        OrderStatus status,
        LocalDateTime createdAt
    );
}
