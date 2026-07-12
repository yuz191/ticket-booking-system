package com.yuqing.ticketbooking.scheduler;

import com.yuqing.ticketbooking.entity.OrderStatus;
import com.yuqing.ticketbooking.entity.TicketOrder;
import com.yuqing.ticketbooking.messaging.OrderCreatedMessage;
import com.yuqing.ticketbooking.messaging.OrderProducer;
import com.yuqing.ticketbooking.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PendingOrderRetryScheduler {
    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;

    public PendingOrderRetryScheduler(OrderRepository orderRepository, OrderProducer orderProducer) {
        this.orderRepository = orderRepository;
        this.orderProducer = orderProducer;
    }

    @Scheduled(fixedDelay = 60000)
    public void retryPendingOrders() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(2);

        List<TicketOrder> pendingOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING,
                cutoffTime
        );

        for (TicketOrder order: pendingOrders) {
            OrderCreatedMessage message = new OrderCreatedMessage(
                    order.getId(),
                    order.getEventId(),
                    order.getTicketTypeId(),
                    order.getQuantity(),
                    order.getUserEmail()
            );

            orderProducer.sendOrderCreatedMessage(message);
        }
    }
}
