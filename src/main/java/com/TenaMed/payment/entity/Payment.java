package com.TenaMed.payment.entity;

import com.TenaMed.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_gateway")
    private String paymentGateway;

    @Column(name = "tx_ref", unique = true)
    private UUID txRef;

    @Column(name = "status")
    private String status;

    @Column(name = "refund_amount", precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
