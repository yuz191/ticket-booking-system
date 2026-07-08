package com.yuqing.ticketbooking.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.yuqing.ticketbooking.service.OrderProcessingService;

@Component
public class OrderConsumer {
    private final OrderProcessingService orderProcessingService;

    public OrderConsumer(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_CREATED,
            groupId = "ticket-order-consumer-group"
    )
    public void consume(OrderCreatedMessage message) {
        orderProcessingService.processOrder(message);
    }
}
