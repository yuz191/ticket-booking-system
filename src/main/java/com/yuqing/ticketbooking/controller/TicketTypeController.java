package com.yuqing.ticketbooking.controller;

import com.yuqing.ticketbooking.dto.CreateTicketTypeRequest;
import com.yuqing.ticketbooking.entity.TicketType;
import com.yuqing.ticketbooking.service.TicketTypeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TicketTypeController {
    private final TicketTypeService ticketTypeService;

    private TicketTypeController(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }

    @PostMapping("/ticket-types")
    public TicketType createTicketType(@Valid @RequestBody CreateTicketTypeRequest request) {
        return ticketTypeService.createTicketType(request);
    }

    @GetMapping("/events/{eventId}/ticket-types")
    public List<TicketType> getTicketTypesByEventId(@PathVariable Long eventId) {
        return ticketTypeService.getTicketTypesByEventId(eventId);
    }

    @GetMapping("/ticket-types/{id}")
    public TicketType getTicketType(@PathVariable Long id) {
        return ticketTypeService.getTicketTypeId(id);
    }

}
