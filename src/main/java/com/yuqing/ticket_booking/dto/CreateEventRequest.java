package com.yuqing.ticket_booking.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter

public class CreateEventRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String venue;

    @NotNull
    @Future
    private LocalDateTime eventTime;

}
