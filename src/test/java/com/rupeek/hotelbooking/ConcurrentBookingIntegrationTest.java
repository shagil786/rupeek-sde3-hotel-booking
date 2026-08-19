package com.rupeek.hotelbooking;

import com.rupeek.hotelbooking.adapter.in.web.HotelDtos;
import com.rupeek.hotelbooking.adapter.out.persistence.*;
import com.rupeek.hotelbooking.application.HotelBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:hotelconcurrency", "spring.datasource.username=sa", "app.demo.username=demo", "app.demo.password=test-only-password"})
class ConcurrentBookingIntegrationTest {
    @Autowired HotelBookingService service;
    @Autowired OwnerJpaRepository owners;
    @Autowired PropertyJpaRepository properties;
    @Autowired RoomTypeJpaRepository rooms;

    @Test
    void onlyOneSimultaneousBookingCanReserveSingleInventoryUnit() throws Exception {
        OwnerEntity owner = owners.save(new OwnerEntity(UUID.randomUUID().toString(), "demo"));
        PropertyEntity property = properties.save(new PropertyEntity(UUID.randomUUID().toString(), owner, "Concurrency Hotel", "Bengaluru", "HSR", 4, "wifi"));
        RoomTypeEntity room = rooms.save(new RoomTypeEntity(UUID.randomUUID().toString(), property, "Deluxe", 2, new BigDecimal("100"), 1));
        var request = new HotelDtos.CreateBookingRequest(room.getId(), LocalDate.of(2030, 1, 10), LocalDate.of(2030, 1, 12), 2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> attempts = List.of(
                    executor.submit(() -> attempt(start, request, "concurrent-a")),
                    executor.submit(() -> attempt(start, request, "concurrent-b")));
            start.countDown();
            List<Object> results = attempts.stream().map(this::get).toList();
            assertThat(results.stream().filter(HotelDtos.BookingResponse.class::isInstance)).hasSize(1);
            assertThat(results.stream().filter(com.rupeek.hotelbooking.adapter.in.web.ApiException.class::isInstance)
                    .map(com.rupeek.hotelbooking.adapter.in.web.ApiException.class::cast)
                    .map(com.rupeek.hotelbooking.adapter.in.web.ApiException::getCode)).containsExactly("ROOM_UNAVAILABLE");
        } finally {
            executor.shutdownNow();
        }
    }

    private Object attempt(CountDownLatch start, HotelDtos.CreateBookingRequest request, String key) throws InterruptedException {
        start.await(5, TimeUnit.SECONDS);
        try { return service.createBooking(request, "demo", key); }
        catch (Throwable error) { return error; }
    }

    private Object get(Future<Object> future) {
        try { return future.get(10, TimeUnit.SECONDS); }
        catch (Exception error) { return error; }
    }
}
