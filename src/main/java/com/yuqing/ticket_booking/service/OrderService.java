package com.yuqing.ticket_booking.service;

import com.yuqing.ticket_booking.dto.CreateOrderRequest;
import com.yuqing.ticket_booking.entity.OrderStatus;
import com.yuqing.ticket_booking.entity.TicketOrder;
import com.yuqing.ticket_booking.entity.TicketType;
import com.yuqing.ticket_booking.repository.EventRepository;
import com.yuqing.ticket_booking.repository.OrderRepository;
import com.yuqing.ticket_booking.repository.TicketTypeRepository;

import java.math.BigDecimal;


public class OrderService {
    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;

    private OrderService(OrderRepository orderRepository, TicketTypeRepository ticketTypeRepository, EventRepository eventRepository) {
        this.orderRepository = orderRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.eventRepository = eventRepository;
    }

    public TicketOrder createTicketOrder(CreateOrderRequest request) {
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
        ticketOrder.setTicketTypeId(request.getTicketTypeId());
        ticketOrder.setQuantity(request.getQuantity());
        ticketOrder.setUserEmail(request.getUserEmail());
        ticketOrder.setTotalPrice(
                ticketType.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()))
        );
        ticketOrder.setStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(ticketOrder);
    }
}
