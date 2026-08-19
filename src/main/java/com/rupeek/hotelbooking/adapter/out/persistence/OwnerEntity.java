package com.rupeek.hotelbooking.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "owners")
public class OwnerEntity {
    @Id private String id;
    private String username;
    private Instant createdAt;

    protected OwnerEntity() {}
    public OwnerEntity(String id, String username) { this.id = id; this.username = username; this.createdAt = Instant.now(); }
    public String getId() { return id; }
    public String getUsername() { return username; }
}
