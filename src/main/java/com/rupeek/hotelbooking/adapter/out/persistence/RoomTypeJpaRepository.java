package com.rupeek.hotelbooking.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RoomTypeJpaRepository extends JpaRepository<RoomTypeEntity, String> { List<RoomTypeEntity> findByPropertyId(String propertyId); }
