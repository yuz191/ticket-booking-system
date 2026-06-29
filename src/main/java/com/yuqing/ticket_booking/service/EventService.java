package com.yuqing.ticket_booking.service;

import com.yuqing.ticket_booking.dto.CreateEventRequest;
import com.yuqing.ticket_booking.entity.Event;
import com.yuqing.ticket_booking.repository.EventRepository;

import java.util.List;

public class EventService {

    private final EventRepository eventRepository;

    private EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Event createEvent(CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setEventTime(request.getEventTime());
        event.setVenue(request.getVenue());

        return eventRepository.save(event);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(Long id) {
            return eventRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
    }
}
