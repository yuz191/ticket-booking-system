package com.yuqing.ticketbooking.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class OrderCreatedMessage {

    private Long orderId;

    private Long eventId;

    private Long ticketTypeId;

    private Integer quantity;

    private String userEmail;
}
