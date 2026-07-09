package com.yuqing.ticketbooking.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCreatedEventListener {

    private final OrderProducer orderProducer;

    public OrderCreatedEventListener(OrderProducer orderProducer) {
        this.orderProducer = orderProducer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        orderProducer.sendOrderCreatedMessage(event.getMessage());
    }
}
