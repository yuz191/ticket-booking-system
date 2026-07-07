package com.yuqing.ticketbooking.service;

import com.yuqing.ticketbooking.dto.CreateOrderRequest;
import com.yuqing.ticketbooking.entity.Event;
import com.yuqing.ticketbooking.entity.TicketOrder;
import com.yuqing.ticketbooking.entity.TicketType;
import com.yuqing.ticketbooking.repository.EventRepository;
import com.yuqing.ticketbooking.repository.OrderRepository;
import com.yuqing.ticketbooking.repository.TicketTypeRepository;
import com.yuqing.ticketbooking.util.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class OrderConcurrencyTest {

    private final OrderService orderService;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final OrderRepository orderRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    OrderConcurrencyTest(OrderService orderService,
                         EventRepository eventRepository,
                         TicketTypeRepository ticketTypeRepository,
                         OrderRepository orderRepository, StringRedisTemplate stringRedisTemplate) {
        this.orderService = orderService;
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.orderRepository = orderRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Test
    void shouldNotOversellWhenManyUsersBuySameTicketType() throws InterruptedException {
        // 1. prepare test data
        Event event = new Event();
        event.setName("Concurrency Test Event");
        event.setDescription("Test event");
        event.setVenue("Test Venue");
        event.setEventTime(LocalDateTime.now().plusDays(10));
        Event savedEvent = eventRepository.save(event);

        TicketType ticketType = new TicketType();
        ticketType.setEventId(savedEvent.getId());
        ticketType.setName("VIP");
        ticketType.setPrice(new BigDecimal("1000.00"));
        ticketType.setTotalQuantity(5);
        ticketType.setAvailableQuantity(5);
        TicketType savedTicketType = ticketTypeRepository.save(ticketType);

        String stockKey = RedisKeyUtil.ticketTypeStockKey(savedTicketType.getTicketTypeId());
        stringRedisTemplate.opsForValue().set(stockKey, savedTicketType.getAvailableQuantity().toString());

        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<TicketOrder> successfulOrders = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // 2. create 20 concurrent tasks
        for (int i = 0; i < threadCount; i++) {
            int userIndex = i;

            executorService.submit(() -> {
                try {
                    // let every thread wait, until startLatch
                    startLatch.await();

                    // After phase 2, OrderService get user email from SecurityContextHolder
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    "user" + userIndex + "@example.com",
                                    null,
                                    List.of()
                            )
                    );

                    CreateOrderRequest request = new CreateOrderRequest();
                    request.setEventId(savedEvent.getId());
                    request.setTicketTypeId(savedTicketType.getTicketTypeId());
                    request.setQuantity(1);

                    TicketOrder order = orderService.createTicketOrder(request);
                    successfulOrders.add(order);

                } catch (Exception ex) {
                    exceptions.add(ex);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });
        }

        // 3. Start the latch, 20 threads create order requests at the same time
        startLatch.countDown();

        // 4. Wait till all threads done
        doneLatch.await();
        executorService.shutdown();

        // 5. Check the stock at the end
        TicketType finalTicketType = ticketTypeRepository.findById(savedTicketType.getTicketTypeId())
                .orElseThrow();

        String redisStock = stringRedisTemplate.opsForValue()
                .get(RedisKeyUtil.ticketTypeStockKey(savedTicketType.getTicketTypeId()));

        System.out.println("Successful orders: " + successfulOrders.size());
        System.out.println("Failed requests: " + exceptions.size());
        System.out.println("Final available quantity: " + finalTicketType.getAvailableQuantity());
        System.out.println("Redis stack: " + redisStock);

        assertEquals(5, successfulOrders.size());
        assertEquals(0, finalTicketType.getAvailableQuantity());
        assertEquals("0", redisStock);
    }
}