package com.rupeek.hotelbooking.adapter.out.persistence;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "properties")
public class PropertyEntity {
    @Id private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id") private OwnerEntity owner;
    private String name;
    private String city;
    private String locality;
    private int starRating;
    private String amenities;
    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY) private Set<RoomTypeEntity> roomTypes = new HashSet<>();

    protected PropertyEntity() {}
    public PropertyEntity(String id, OwnerEntity owner, String name, String city, String locality, int starRating, String amenities) {
        this.id=id; this.owner=owner; this.name=name; this.city=city; this.locality=locality; this.starRating=starRating; this.amenities=amenities;
    }
    public String getId(){return id;} public OwnerEntity getOwner(){return owner;} public String getName(){return name;}
    public String getCity(){return city;} public String getLocality(){return locality;} public int getStarRating(){return starRating;}
    public String getAmenities(){return amenities;}
}
