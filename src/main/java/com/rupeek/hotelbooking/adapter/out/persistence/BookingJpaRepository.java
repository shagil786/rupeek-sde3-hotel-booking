package com.rupeek.hotelbooking.adapter.out.persistence;
import com.rupeek.hotelbooking.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface BookingJpaRepository extends JpaRepository<BookingEntity, String> {
    Optional<BookingEntity> findByCustomerUsernameAndIdempotencyKey(String username, String key);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BookingEntity b where b.id = :id")
    Optional<BookingEntity> findByIdForUpdate(@Param("id") String id);
    List<BookingEntity> findByStatusAndCreatedAtBefore(BookingStatus status, Instant cutoff);
    List<BookingEntity> findByRoomTypeIdAndStatusInAndCheckInLessThanAndCheckOutGreaterThan(String roomTypeId, List<BookingStatus> statuses, LocalDate checkOut, LocalDate checkIn);
}
