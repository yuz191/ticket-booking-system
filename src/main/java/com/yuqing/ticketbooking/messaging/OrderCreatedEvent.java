package com.yuqing.ticketbooking.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderCreatedEvent {

    private OrderCreatedMessage message;
}