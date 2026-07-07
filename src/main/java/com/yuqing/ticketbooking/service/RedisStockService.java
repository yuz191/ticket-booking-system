package com.yuqing.ticketbooking.service;

import com.yuqing.ticketbooking.util.RedisKeyUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class RedisStockService {
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> decreaseStockScript;


    public RedisStockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.decreaseStockScript = new DefaultRedisScript<>();
        this.decreaseStockScript.setLocation(
                new ClassPathResource("lua/decrease_stock.lua")
        );
        this.decreaseStockScript.setResultType(Long.class);
    }

    public void decreaseStock(Long ticketTypeId, Integer quantity) {
        String stockKey = RedisKeyUtil.ticketTypeStockKey(ticketTypeId);

        Long result = stringRedisTemplate.execute(
                decreaseStockScript,
                Collections.singletonList(stockKey),
                quantity.toString()
        );

        if (result == null) {
            throw new RuntimeException("Redis stock operation failed");
        }

        if (result.equals(-1L)) {
            throw new RuntimeException("Redis stock key not found: " + stockKey);
        }

        if (result.equals(-2L)) {
            throw new RuntimeException("Not enough tickets available");
        }

        if (result.equals(-3L)) {
            throw new RuntimeException("Invalid ticket quantity");
        }

        if (result.equals(-4L)) {
            throw new RuntimeException("Redis stock value is not a number: " + stockKey);
        }
    }

    public void increaseStock(Long ticketTypeId, Integer quantity) {
        String stockKey = RedisKeyUtil.ticketTypeStockKey(ticketTypeId);

        stringRedisTemplate.opsForValue().increment(stockKey, quantity);
    }

    public Integer getStock(Long ticketTypeId) {
        String stockKey = RedisKeyUtil.ticketTypeStockKey(ticketTypeId);

        String value = stringRedisTemplate.opsForValue().get(stockKey);

        if (value == null) {
            throw new RuntimeException("Redis stock key not found: " + stockKey);
        }

        return Integer.valueOf(value);
    }
}
