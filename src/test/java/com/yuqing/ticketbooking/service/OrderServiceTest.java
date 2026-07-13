package com.yuqing.ticketbooking.service;

import com.yuqing.ticketbooking.dto.CreateOrderRequest;
import com.yuqing.ticketbooking.entity.OrderStatus;
import com.yuqing.ticketbooking.entity.TicketOrder;
import com.yuqing.ticketbooking.entity.TicketType;
import com.yuqing.ticketbooking.messaging.OrderCreatedEvent;
import com.yuqing.ticketbooking.repository.OrderRepository;
import com.yuqing.ticketbooking.repository.TicketTypeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private RedisStockService redisStockService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, ticketTypeRepository, redisStockService, eventPublisher);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTicketOrderShouldSavePendingOrderAndPublishEvent() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer@example.com", null, List.of())
        );

        CreateOrderRequest request = new CreateOrderRequest();
        request.setEventId(10L);
        request.setTicketTypeId(20L);
        request.setQuantity(2);

        TicketType ticketType = new TicketType();
        ticketType.setTicketTypeId(20L);
        ticketType.setEventId(10L);
        ticketType.setPrice(new BigDecimal("75.50"));

        when(ticketTypeRepository.findById(20L)).thenReturn(Optional.of(ticketType));
        when(orderRepository.save(any(TicketOrder.class))).thenAnswer(invocation -> {
            TicketOrder order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        TicketOrder savedOrder = orderService.createTicketOrder(request);

        ArgumentCaptor<TicketOrder> orderCaptor = ArgumentCaptor.forClass(TicketOrder.class);
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);

        verify(orderRepository).save(orderCaptor.capture());
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        TicketOrder persistedOrder = orderCaptor.getValue();
        assertEquals("buyer@example.com", persistedOrder.getUserEmail());
        assertEquals(2, persistedOrder.getQuantity());
        assertEquals(new BigDecimal("151.00"), persistedOrder.getTotalPrice());
        assertEquals(OrderStatus.PENDING, persistedOrder.getStatus());

        assertEquals(99L, savedOrder.getId());
        assertEquals(99L, eventCaptor.getValue().getMessage().getOrderId());
        assertEquals(20L, eventCaptor.getValue().getMessage().getTicketTypeId());
        verify(redisStockService).decreaseStock(20L, 2);
        verify(redisStockService, never()).increaseStock(20L, 2);
    }

    @Test
    void createTicketOrderShouldRestoreRedisStockWhenTicketTypeDoesNotMatchEvent() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer@example.com", null, List.of())
        );

        CreateOrderRequest request = new CreateOrderRequest();
        request.setEventId(10L);
        request.setTicketTypeId(20L);
        request.setQuantity(1);

        TicketType ticketType = new TicketType();
        ticketType.setTicketTypeId(20L);
        ticketType.setEventId(999L);
        ticketType.setPrice(new BigDecimal("75.50"));

        when(ticketTypeRepository.findById(20L)).thenReturn(Optional.of(ticketType));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.createTicketOrder(request));

        assertEquals("Ticket type does not belong to this event.", exception.getMessage());
        verify(redisStockService).decreaseStock(20L, 1);
        verify(redisStockService).increaseStock(20L, 1);
        verify(orderRepository, never()).save(any(TicketOrder.class));
    }
}
