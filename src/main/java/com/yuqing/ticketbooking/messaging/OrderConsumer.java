package com.yuqing.ticketbooking.messaging;

import com.yuqing.ticketbooking.service.OrderProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    private final OrderProcessingService orderProcessingService;

    public OrderConsumer(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_CREATED,
            groupId = "ticket-order-consumer-group"
    )
    public void consume(OrderCreatedMessage message) {
        log.info("Consumed order-created message for orderId={}, ticketTypeId={}, quantity={}",
                message.getOrderId(),
                message.getTicketTypeId(),
                message.getQuantity());
        orderProcessingService.processOrder(message);
    }
}
