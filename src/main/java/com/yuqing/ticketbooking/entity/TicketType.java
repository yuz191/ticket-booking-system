package com.yuqing.ticketbooking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ticket_types")
public class TicketType {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_type_seq")
    @SequenceGenerator(
            name = "ticket_type_seq",
            sequenceName = "ticket_type_sequence",
            allocationSize = 50
    )
    private Long ticketTypeId;

    @Column(nullable = false, name = "event_id")
    private Long eventId;

    @Column(nullable = false, name = "totalQuantity")
    private Integer totalQuantity;

    @Column(nullable = false, name = "available_quantity")
    private Integer availableQuantity;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, scale = 2, precision = 10)
    private BigDecimal price;

    @Column(nullable = false, updatable = false, name = "create_at")
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (availableQuantity == null) {
            this.availableQuantity = this.totalQuantity;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
