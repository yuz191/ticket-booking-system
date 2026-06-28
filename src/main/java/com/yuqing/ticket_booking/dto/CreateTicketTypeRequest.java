package com.yuqing.ticket_booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class CreateTicketTypeRequest {
    @NotBlank
    private Long eventId;

    @NotBlank
    private String name;

    @NotBlank
    @DecimalMin("0.00")
    private BigDecimal price;

    @NotBlank
    @Min(1)
    private Integer totalQuantity;
}
