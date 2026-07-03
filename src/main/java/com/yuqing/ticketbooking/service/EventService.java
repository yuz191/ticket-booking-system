package com.yuqing.ticketbooking.service;

import com.yuqing.ticketbooking.dto.CreateEventRequest;
import com.yuqing.ticketbooking.entity.Event;
import com.yuqing.ticketbooking.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
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
