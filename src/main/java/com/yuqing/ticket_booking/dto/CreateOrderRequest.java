package com.yuqing.ticket_booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CreateOrderRequest {
    @NotBlank
    private Long eventId;

    @NotBlank
    private Long ticketTypeId;

    @NotBlank
    @Min(1)
    private Integer quantity;

    @NotBlank
    @Email()
    private String userEmail;
}
