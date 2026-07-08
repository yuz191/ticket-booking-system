package com.yuqing.ticketbooking.service;

import com.yuqing.ticketbooking.entity.OrderStatus;
import com.yuqing.ticketbooking.entity.TicketOrder;
import com.yuqing.ticketbooking.entity.TicketType;
import com.yuqing.ticketbooking.messaging.OrderCreatedMessage;
import com.yuqing.ticketbooking.repository.OrderRepository;
import com.yuqing.ticketbooking.repository.TicketTypeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessingService {

    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final RedisStockService redisStockService;


    public OrderProcessingService(OrderRepository orderRepository, TicketTypeRepository ticketTypeRepository, RedisStockService redisStockService) {
        this.orderRepository = orderRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.redisStockService = redisStockService;
    }

    @Transactional
    public void processOrder(OrderCreatedMessage message) {
        TicketOrder order = orderRepository.findById(message.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found."));

        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        try {
            TicketType ticketType = ticketTypeRepository.findWithLockByTicketTypeId(
                    message.getTicketTypeId()
            ).orElseThrow(() -> new RuntimeException("Ticket type not found."));

            if (ticketType.getAvailableQuantity() < message.getQuantity()) {
                order.setStatus(OrderStatus.FAILED);

                redisStockService.increaseStock(
                        message.getTicketTypeId(),
                        message.getQuantity()
                );

                return;
            }

            ticketType.setAvailableQuantity(
                    ticketType.getAvailableQuantity() - message.getQuantity()
            );

            order.setStatus(OrderStatus.CONFIRMED);
        } catch (RuntimeException ex) {
            redisStockService.increaseStock(
                    message.getTicketTypeId(),
                    message.getQuantity()
            );

            throw ex;
        }
    }
}
