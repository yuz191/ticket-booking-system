package com.yuqing.ticketbooking.service;

import com.yuqing.ticketbooking.dto.CreateOrderRequest;
import com.yuqing.ticketbooking.entity.OrderStatus;
import com.yuqing.ticketbooking.entity.TicketOrder;
import com.yuqing.ticketbooking.entity.TicketType;
import com.yuqing.ticketbooking.messaging.OrderCreatedEvent;
import com.yuqing.ticketbooking.messaging.OrderCreatedMessage;
import com.yuqing.ticketbooking.messaging.OrderProducer;
import com.yuqing.ticketbooking.repository.EventRepository;
import com.yuqing.ticketbooking.repository.OrderRepository;
import com.yuqing.ticketbooking.repository.TicketTypeRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final RedisStockService redisStockService;
//    private final OrderProducer orderProducer;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, TicketTypeRepository ticketTypeRepository, RedisStockService redisStockService, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.redisStockService = redisStockService;
//        this.orderProducer = orderProducer;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TicketOrder createTicketOrder(CreateOrderRequest request) {
        // Redis - reduce stock atomically
        redisStockService.decreaseStock(
                request.getTicketTypeId(),
                request.getQuantity()
        );

        try {
            String currentUserEmail = getCurrentUserEmail();

            TicketType ticketType = ticketTypeRepository.findById(request.getTicketTypeId())
                    .orElseThrow(() -> new RuntimeException("Ticket type not found"));

            if (!ticketType.getEventId().equals(request.getEventId())) {
                throw new RuntimeException("Ticket type does not belong to this event.");
            }

//            if (ticketType.getAvailableQuantity() < request.getQuantity()) {
//                throw new RuntimeException("Not enough tickets available");
//            }

              // Kafka will reduce available quantity asynchronously
//            ticketType.setAvailableQuantity(
//                    ticketType.getAvailableQuantity() - request.getQuantity()
//            );
//
//            ticketTypeRepository.save(ticketType);

            TicketOrder ticketOrder = new TicketOrder();
            ticketOrder.setEventId(request.getEventId());
            ticketOrder.setTicketTypeId(ticketType.getTicketTypeId());
            ticketOrder.setQuantity(request.getQuantity());
            ticketOrder.setUserEmail(currentUserEmail);
            ticketOrder.setTotalPrice(
                    ticketType.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()))
            );
            ticketOrder.setStatus(OrderStatus.PENDING);

            TicketOrder savedOrder =  orderRepository.save(ticketOrder);

            OrderCreatedMessage message = new OrderCreatedMessage(
                    savedOrder.getId(),
                    savedOrder.getEventId(),
                    savedOrder.getTicketTypeId(),
                    savedOrder.getQuantity(),
                    savedOrder.getUserEmail()
            );

            eventPublisher.publishEvent(new OrderCreatedEvent(message));

            return savedOrder;

        } catch (RuntimeException ex) {
            redisStockService.increaseStock(
                    request.getTicketTypeId(),
                    request.getQuantity()
            );

            throw ex;
        }
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

    public TicketOrder getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}
