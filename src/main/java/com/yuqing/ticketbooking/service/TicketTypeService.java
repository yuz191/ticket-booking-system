package com.yuqing.ticketbooking.service;

import com.yuqing.ticketbooking.dto.CreateTicketTypeRequest;
import com.yuqing.ticketbooking.entity.Event;
import com.yuqing.ticketbooking.entity.TicketType;
import com.yuqing.ticketbooking.repository.EventRepository;
import com.yuqing.ticketbooking.repository.TicketTypeRepository;
import com.yuqing.ticketbooking.util.RedisKeyUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketTypeService {
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisStockService redisStockService;

    private TicketTypeService(EventRepository eventRepository, TicketTypeRepository ticketTypeRepository, StringRedisTemplate stringRedisTemplate, RedisStockService redisStockService) {
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisStockService = redisStockService;
    }

    public TicketType createTicketType(CreateTicketTypeRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found."));

        TicketType ticketType = new TicketType();
        ticketType.setEventId(event.getId());
        ticketType.setName(request.getName());
        ticketType.setPrice(request.getPrice());
        ticketType.setTotalQuantity(request.getTotalQuantity());

        TicketType savedTicketType = ticketTypeRepository.save(ticketType);

        String stockKey = RedisKeyUtil.ticketTypeStockKey(savedTicketType.getTicketTypeId());

        stringRedisTemplate.opsForValue().set(stockKey, savedTicketType.getAvailableQuantity().toString());

        return savedTicketType;
    }


    public List<TicketType> getTicketTypesByEventId(Long eventId) {
        boolean eventExist = eventRepository.existsById(eventId);

        if (!eventExist) {
            throw new RuntimeException("Event not found.");
        }

        return ticketTypeRepository.findByEventId(eventId);
    }

    public TicketType getTicketTypeId(Long id) {
        return ticketTypeRepository.findById(id).
                orElseThrow(() -> new RuntimeException("Ticket type not found."));
    }

    public Integer getRedisStock(Long ticketTypeId) {
        return redisStockService.getStock(ticketTypeId);
    }
}
