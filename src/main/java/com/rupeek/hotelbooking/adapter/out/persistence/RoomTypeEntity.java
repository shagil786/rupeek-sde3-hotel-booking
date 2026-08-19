package com.rupeek.hotelbooking.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "room_types")
public class RoomTypeEntity {
    @Id private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "property_id") private PropertyEntity property;
    private String name;
    private int capacity;
    private BigDecimal pricePerNight;
    private int inventoryCount;
    @Version private long version;
    private Instant lastBookingMutation;

    protected RoomTypeEntity() {}
    public RoomTypeEntity(String id, PropertyEntity property, String name, int capacity, BigDecimal pricePerNight, int inventoryCount) {
        this.id=id; this.property=property; this.name=name; this.capacity=capacity; this.pricePerNight=pricePerNight; this.inventoryCount=inventoryCount;
    }
    public String getId(){return id;} public PropertyEntity getProperty(){return property;} public String getName(){return name;}
    public int getCapacity(){return capacity;} public BigDecimal getPricePerNight(){return pricePerNight;} public int getInventoryCount(){return inventoryCount;}
    public long getVersion(){return version;}
    public void markBookingMutation(){this.lastBookingMutation=Instant.now();}
}
