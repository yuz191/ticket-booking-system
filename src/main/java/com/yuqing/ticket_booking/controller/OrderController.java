package com.yuqing.ticket_booking.controller;

import com.yuqing.ticket_booking.dto.CreateOrderRequest;
import com.yuqing.ticket_booking.entity.TicketOrder;
import com.yuqing.ticket_booking.service.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")

public class OrderController {
    private final OrderService orderService;

    private OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public TicketOrder createTicketOrder(CreateOrderRequest request) {
        return orderService.createTicketOrder(request);
    }
}
