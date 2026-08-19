package com.rupeek.hotelbooking.adapter.out.persistence;
import com.rupeek.hotelbooking.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface BookingJpaRepository extends JpaRepository<BookingEntity, String> {
    Optional<BookingEntity> findByCustomerUsernameAndIdempotencyKey(String username, String key);
    List<BookingEntity> findByStatusAndCreatedAtBefore(BookingStatus status, Instant cutoff);
    List<BookingEntity> findByRoomTypeIdAndStatusInAndCheckInLessThanAndCheckOutGreaterThan(String roomTypeId, List<BookingStatus> statuses, LocalDate checkOut, LocalDate checkIn);
}
