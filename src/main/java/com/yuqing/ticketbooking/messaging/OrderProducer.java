package com.yuqing.ticketbooking.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {
    private final KafkaTemplate<String, OrderCreatedMessage> kafkaTemplate;

    public OrderProducer(KafkaTemplate<String, OrderCreatedMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreatedMessage(OrderCreatedMessage message) {
        kafkaTemplate.send(
                KafkaTopics.ORDER_CREATED,
                message.getOrderId().toString(),
                message
        );
    }
}
