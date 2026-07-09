package com.yuqing.ticketbooking.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {
    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);

    private final KafkaTemplate<String, OrderCreatedMessage> kafkaTemplate;

    public OrderProducer(KafkaTemplate<String, OrderCreatedMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreatedMessage(OrderCreatedMessage message) {
        log.info("Sending order-created message for orderId={}, ticketTypeId={}, quantity={}",
                message.getOrderId(),
                message.getTicketTypeId(),
                message.getQuantity());
        kafkaTemplate.send(
                KafkaTopics.ORDER_CREATED,
                message.getOrderId().toString(),
                message
        );
    }
}
