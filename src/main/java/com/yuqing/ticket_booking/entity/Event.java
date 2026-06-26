package com.yuqing.ticket_booking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_seq")
    @SequenceGenerator(
            name = "event_seq",
            sequenceName = "event_sequence",
            allocationSize = 50
    )
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    String description;
}
