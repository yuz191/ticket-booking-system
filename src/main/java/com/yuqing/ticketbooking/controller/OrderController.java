package com.yuqing.ticketbooking.controller;

import com.yuqing.ticketbooking.dto.CreateOrderRequest;
import com.yuqing.ticketbooking.entity.TicketOrder;
import com.yuqing.ticketbooking.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/orders")

public class OrderController {
    private final OrderService orderService;

    private OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public TicketOrder createTicketOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createTicketOrder(request);
    }

    @GetMapping("/{id}")
    public TicketOrder getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }
}
