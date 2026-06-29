package com.yuqing.ticket_booking.controller;

import com.yuqing.ticket_booking.dto.CreateEventRequest;
import com.yuqing.ticket_booking.entity.Event;
import com.yuqing.ticket_booking.service.EventService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")

public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public Event createEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    @GetMapping
    public List<Event> getAllEvents() {
        return eventService.getAllEvents();
    }

    @GetMapping("/{id}")
    public Event getEventById(@PathVariable Long id) {
        return eventService.getEventById(id);
    }


}
