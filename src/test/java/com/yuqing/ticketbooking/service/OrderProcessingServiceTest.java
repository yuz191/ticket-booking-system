package com.yuqing.ticketbooking.service;

import com.yuqing.ticketbooking.entity.OrderStatus;
import com.yuqing.ticketbooking.entity.TicketOrder;
import com.yuqing.ticketbooking.entity.TicketType;
import com.yuqing.ticketbooking.messaging.OrderCreatedMessage;
import com.yuqing.ticketbooking.repository.OrderRepository;
import com.yuqing.ticketbooking.repository.TicketTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private RedisStockService redisStockService;

    private OrderProcessingService orderProcessingService;

    @BeforeEach
    void setUp() {
        orderProcessingService = new OrderProcessingService(orderRepository, ticketTypeRepository, redisStockService);
    }

    @Test
    void processOrderShouldConfirmPendingOrderAndDecreaseDatabaseStock() {
        OrderCreatedMessage message = new OrderCreatedMessage(1L, 10L, 20L, 2, "buyer@example.com");

        TicketOrder order = new TicketOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);

        TicketType ticketType = new TicketType();
        ticketType.setTicketTypeId(20L);
        ticketType.setAvailableQuantity(5);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(ticketTypeRepository.findWithLockByTicketTypeId(20L)).thenReturn(Optional.of(ticketType));

        orderProcessingService.processOrder(message);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(3, ticketType.getAvailableQuantity());
        verify(redisStockService, never()).increaseStock(20L, 2);
    }

    @Test
    void processOrderShouldMarkOrderFailedAndRestoreRedisStockWhenDbStockIsInsufficient() {
        OrderCreatedMessage message = new OrderCreatedMessage(1L, 10L, 20L, 3, "buyer@example.com");

        TicketOrder order = new TicketOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);

        TicketType ticketType = new TicketType();
        ticketType.setTicketTypeId(20L);
        ticketType.setAvailableQuantity(1);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(ticketTypeRepository.findWithLockByTicketTypeId(20L)).thenReturn(Optional.of(ticketType));

        orderProcessingService.processOrder(message);

        assertEquals(OrderStatus.FAILED, order.getStatus());
        assertEquals(1, ticketType.getAvailableQuantity());
        verify(redisStockService).increaseStock(20L, 3);
    }

    @Test
    void processOrderShouldIgnoreOrdersThatAreAlreadyProcessed() {
        OrderCreatedMessage message = new OrderCreatedMessage(1L, 10L, 20L, 1, "buyer@example.com");

        TicketOrder order = new TicketOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderProcessingService.processOrder(message);

        verify(ticketTypeRepository, never()).findWithLockByTicketTypeId(20L);
        verify(redisStockService, never()).increaseStock(20L, 1);
    }
}
