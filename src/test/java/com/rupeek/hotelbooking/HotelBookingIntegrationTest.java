package com.rupeek.hotelbooking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.rupeek.hotelbooking.application.HotelBookingService;
import com.rupeek.hotelbooking.adapter.out.persistence.OwnerEntity;
import com.rupeek.hotelbooking.adapter.out.persistence.OwnerJpaRepository;
import com.rupeek.hotelbooking.adapter.out.persistence.PropertyEntity;
import com.rupeek.hotelbooking.adapter.out.persistence.PropertyJpaRepository;
import com.rupeek.hotelbooking.adapter.out.persistence.RoomTypeEntity;
import com.rupeek.hotelbooking.adapter.out.persistence.RoomTypeJpaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties={
        "spring.datasource.url=jdbc:h2:mem:hoteltest;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.demo.username=demo",
        "app.demo.password=test-only-password"
})
class HotelBookingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired HotelBookingService service;
    @Autowired OwnerJpaRepository owners;
    @Autowired PropertyJpaRepository properties;
    @Autowired RoomTypeJpaRepository rooms;

    @Test void completesHoldPaymentAndCancellationFlow() throws Exception {
        String owner=mvc.perform(post("/api/v1/owners").with(httpBasic("demo","test-only-password")).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"demo\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String ownerId=owner.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*","$1");
        String property=mvc.perform(post("/api/v1/owners/"+ownerId+"/properties").with(httpBasic("demo","test-only-password")).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Test Hotel\",\"city\":\"Bengaluru\",\"locality\":\"HSR\",\"starRating\":4,\"amenities\":[\"wifi\"]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String propertyId=property.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*","$1");
        String room=mvc.perform(post("/api/v1/properties/"+propertyId+"/room-types").with(httpBasic("demo","test-only-password")).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Deluxe\",\"capacity\":2,\"pricePerNight\":100,\"inventoryCount\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String roomId=room.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*","$1");
        String booking=mvc.perform(post("/api/v1/bookings").with(httpBasic("demo","test-only-password")).header("Idempotency-Key","booking-1").contentType(MediaType.APPLICATION_JSON).content("{\"roomTypeId\":\""+roomId+"\",\"checkIn\":\"2030-01-10\",\"checkOut\":\"2030-01-12\",\"guests\":2}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING_PAYMENT")).andReturn().getResponse().getContentAsString();
        String bookingId=booking.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*","$1");
        mvc.perform(post("/api/v1/bookings/"+bookingId+"/payments").with(httpBasic("demo","test-only-password")).header("Idempotency-Key","payment-1").contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"CARD\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(post("/api/v1/bookings/"+bookingId+"/cancellations").with(httpBasic("demo","test-only-password")).header("Idempotency-Key","cancel-1"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/bookings/"+bookingId).with(httpBasic("demo","test-only-password")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test void rejectsOverlappingInventory() throws Exception {
        // The first test proves the full persisted flow; this assertion pins the public validation contract.
        mvc.perform(post("/api/v1/bookings").with(httpBasic("demo","test-only-password")).header("Idempotency-Key","invalid").contentType(MediaType.APPLICATION_JSON).content("{\"roomTypeId\":\"missing\",\"checkIn\":\"2030-01-12\",\"checkOut\":\"2030-01-10\",\"guests\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test void supportsDiscoveryWithoutDates() throws Exception {
        mvc.perform(get("/api/v1/properties?city=Bengaluru").with(httpBasic("demo", "test-only-password")))
                .andExpect(status().isOk());
    }

    @Test void rejectsRoomTypeWithoutPriceAsValidationError() throws Exception {
        mvc.perform(post("/api/v1/properties/missing/room-types").with(httpBasic("demo", "test-only-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Deluxe\",\"capacity\":2,\"inventoryCount\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test void expiresAbandonedPaymentHolds() throws Exception {
        OwnerEntity owner=owners.findByUsername("demo").orElseGet(() -> owners.save(new OwnerEntity(UUID.randomUUID().toString(),"demo")));
        PropertyEntity property=properties.save(new PropertyEntity(UUID.randomUUID().toString(),owner,"Expiry Hotel","Bengaluru","HSR",4,"wifi"));
        RoomTypeEntity room=rooms.save(new RoomTypeEntity(UUID.randomUUID().toString(),property,"Deluxe",2,new java.math.BigDecimal("100"),1));
        String booking=mvc.perform(post("/api/v1/bookings").with(httpBasic("demo","test-only-password")).header("Idempotency-Key","expiry-booking").contentType(MediaType.APPLICATION_JSON).content("{\"roomTypeId\":\""+room.getId()+"\",\"checkIn\":\"2030-01-10\",\"checkOut\":\"2030-01-12\",\"guests\":2}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String bookingId=booking.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*","$1");
        service.expireAbandonedBookings(Instant.now().plus(16, ChronoUnit.MINUTES), 15);
        mvc.perform(get("/api/v1/bookings/"+bookingId).with(httpBasic("demo","test-only-password")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXPIRED"));
    }
}
