package com.yuqing.ticketbooking.service;

import com.yuqing.ticketbooking.dto.CreateOrderRequest;
import com.yuqing.ticketbooking.entity.OrderStatus;
import com.yuqing.ticketbooking.entity.TicketOrder;
import com.yuqing.ticketbooking.entity.TicketType;
import com.yuqing.ticketbooking.repository.EventRepository;
import com.yuqing.ticketbooking.repository.OrderRepository;
import com.yuqing.ticketbooking.repository.TicketTypeRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public OrderService(OrderRepository orderRepository, TicketTypeRepository ticketTypeRepository, EventRepository eventRepository) {
        this.orderRepository = orderRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    @Transactional
    public TicketOrder createTicketOrder(CreateOrderRequest request) {
        String currentUserEmail = getCurrentUserEmail();

        TicketType ticketType = ticketTypeRepository.findById(request.getTicketTypeId())
                .orElseThrow(() -> new RuntimeException("Ticket type not found"));

        if (!ticketType.getEventId().equals(request.getEventId())) {
            throw new RuntimeException("Ticket type does not belong to this event.");
        }

        if (ticketType.getAvailableQuantity() < request.getQuantity()) {
            throw new RuntimeException("Not enough tickets available");
        }

        ticketType.setAvailableQuantity(
                ticketType.getAvailableQuantity() - request.getQuantity()
        );

        ticketTypeRepository.save(ticketType);

        TicketOrder ticketOrder = new TicketOrder();
        ticketOrder.setEventId(request.getEventId());
        ticketOrder.setTicketTypeId(ticketType.getTicketTypeId());
        ticketOrder.setQuantity(request.getQuantity());
        ticketOrder.setUserEmail(currentUserEmail);
        ticketOrder.setTotalPrice(
                ticketType.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()))
        );
        ticketOrder.setStatus(OrderStatus.CONFIRMED);

        return orderRepository.save(ticketOrder);
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {
            throw new RuntimeException("User is not authenticated.");
        }

        return authentication.getName();
    }
}
