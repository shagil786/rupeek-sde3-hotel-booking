package com.rupeek.hotelbooking.adapter.out.persistence;

import com.rupeek.hotelbooking.domain.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id private String id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id") private BookingEntity booking;
    private String method;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) private PaymentStatus status;
    private Instant createdAt;
    protected PaymentEntity() {}
    public PaymentEntity(String id, BookingEntity booking, String method, BigDecimal amount, PaymentStatus status) { this.id=id; this.booking=booking; this.method=method; this.amount=amount; this.status=status; this.createdAt=Instant.now(); }
    public PaymentStatus getStatus(){return status;} public void setStatus(PaymentStatus status){this.status=status;}
}
