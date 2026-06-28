package com.yuqing.ticket_booking.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_orders")
@Getter
@Setter

public class TicketOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_order_seq")
    @SequenceGenerator(
            name = "ticket_order_seq",
            sequenceName = "ticket_order_sequence",
            allocationSize = 50
    )
    private Long id;


    @Column(nullable = false)
    private Long eventId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
