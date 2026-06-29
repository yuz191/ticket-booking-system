package com.yuqing.ticket_booking.service;

import com.yuqing.ticket_booking.dto.CreateTicketTypeRequest;
import com.yuqing.ticket_booking.entity.TicketType;
import com.yuqing.ticket_booking.repository.EventRepository;
import com.yuqing.ticket_booking.repository.TicketTypeRepository;

import java.util.List;

public class TicketTypeService {
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;

    private TicketTypeService(EventRepository eventRepository, TicketTypeRepository ticketTypeRepository) {
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    public TicketType createTicketType(CreateTicketTypeRequest request) {
        TicketType ticketType = new TicketType();
        ticketType.setName(request.getName());
        ticketType.setPrice(request.getPrice());
        ticketType.setTotalQuantity(request.getTotalQuantity());

        return ticketTypeRepository.save(ticketType);
    }


    public List<TicketType> getTicketTypesByEventId(Long eventId) {
        boolean eventExist = eventRepository.existsById(eventId);

        if (!eventExist) {
            throw new RuntimeException("Event not found.");
        }

        return ticketTypeRepository.findByEventId(eventId);
    }
}
