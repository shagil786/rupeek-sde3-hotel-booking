package com.rupeek.hotelbooking.adapter.out.persistence;

import com.rupeek.hotelbooking.domain.BookingStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class BookingEntity {
    @Id private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_type_id") private RoomTypeEntity roomType;
    private String customerUsername;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) private BookingStatus status;
    private String idempotencyKey;
    private String cancellationIdempotencyKey;
    @Version private long version;
    private Instant createdAt;

    protected BookingEntity() {}
    public BookingEntity(String id, RoomTypeEntity roomType, String customerUsername, LocalDate checkIn, LocalDate checkOut, int guests, BigDecimal amount, String idempotencyKey) {
        this.id=id; this.roomType=roomType; this.customerUsername=customerUsername; this.checkIn=checkIn; this.checkOut=checkOut; this.guests=guests; this.amount=amount; this.idempotencyKey=idempotencyKey; this.status=BookingStatus.PENDING_PAYMENT; this.createdAt=Instant.now();
    }
    public String getId(){return id;} public RoomTypeEntity getRoomType(){return roomType;} public String getCustomerUsername(){return customerUsername;}
    public LocalDate getCheckIn(){return checkIn;} public LocalDate getCheckOut(){return checkOut;} public int getGuests(){return guests;}
    public BigDecimal getAmount(){return amount;} public BookingStatus getStatus(){return status;} public void setStatus(BookingStatus s){status=s;}
    public String getIdempotencyKey(){return idempotencyKey;}
    public String getCancellationIdempotencyKey(){return cancellationIdempotencyKey;} public void setCancellationIdempotencyKey(String key){cancellationIdempotencyKey=key;}
}
