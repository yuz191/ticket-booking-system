package com.yuqing.ticketbooking.util;

public class RedisKeyUtil {

    private RedisKeyUtil() {
    }

    public static String ticketTypeStockKey(Long ticketTypeId) {
        return "ticket_type:stock:" + ticketTypeId;
    }
}
