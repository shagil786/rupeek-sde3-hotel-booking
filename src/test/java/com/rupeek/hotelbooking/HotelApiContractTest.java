package com.rupeek.hotelbooking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:hotelapi",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.demo.username=demo",
        "app.demo.password=test-only-password"
})
class HotelApiContractTest {
    @Autowired MockMvc mvc;

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/v1/properties"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsStructuredValidationErrors() throws Exception {
        mvc.perform(post("/api/v1/owners")
                        .with(httpBasic("demo", "test-only-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void rejectsUnknownFieldsAndInvalidTypes() throws Exception {
        mvc.perform(post("/api/v1/owners")
                        .with(httpBasic("demo", "test-only-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"unexpected\":true}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/owners")
                        .with(httpBasic("demo", "test-only-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":{}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingIdempotencyKey() throws Exception {
        mvc.perform(post("/api/v1/bookings")
                        .with(httpBasic("demo", "test-only-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomTypeId\":\"missing\",\"checkIn\":\"2030-01-10\",\"checkOut\":\"2030-01-12\",\"guests\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsStructuredErrorForMissingIdempotencyKey() throws Exception {
        mvc.perform(post("/api/v1/bookings").with(httpBasic("demo", "test-only-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomTypeId\":\"missing\",\"checkIn\":\"2030-01-10\",\"checkOut\":\"2030-01-12\",\"guests\":1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    void repeatsFailedPaymentWithTheSameKeyWithoutChargingAgain() throws Exception {
        String ownerId = idOf(mvc.perform(post("/api/v1/owners").with(httpBasic("demo", "test-only-password"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"demo\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String propertyId = idOf(mvc.perform(post("/api/v1/owners/" + ownerId + "/properties").with(httpBasic("demo", "test-only-password"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Retry Hotel\",\"city\":\"Bengaluru\",\"locality\":\"HSR\",\"starRating\":4,\"amenities\":[\"wifi\"]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String roomId = idOf(mvc.perform(post("/api/v1/properties/" + propertyId + "/room-types").with(httpBasic("demo", "test-only-password"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Deluxe\",\"capacity\":2,\"pricePerNight\":100,\"inventoryCount\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String booking = mvc.perform(post("/api/v1/bookings").with(httpBasic("demo", "test-only-password"))
                        .header("Idempotency-Key", "failed-booking").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomTypeId\":\"" + roomId + "\",\"checkIn\":\"2030-04-10\",\"checkOut\":\"2030-04-12\",\"guests\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String bookingId = idOf(booking);
        mvc.perform(post("/api/v1/bookings/" + bookingId + "/payments").with(httpBasic("demo", "test-only-password"))
                        .header("Idempotency-Key", "failed-payment").contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"FAIL\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));
        mvc.perform(post("/api/v1/bookings/" + bookingId + "/payments").with(httpBasic("demo", "test-only-password"))
                        .header("Idempotency-Key", "failed-payment").contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"CARD\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));
    }

    @Test
    void returnsPaginationHeadersForPropertySearch() throws Exception {
        mvc.perform(get("/api/v1/properties?page=0&size=1").with(httpBasic("demo", "test-only-password")))
                .andExpect(status().isOk()).andExpect(header().string("X-Page", "0"))
                .andExpect(header().string("X-Page-Size", "1")).andExpect(header().string("X-Total-Count", "0"))
                .andExpect(header().string("X-Has-More", "false"));
    }

    @Test
    void permitsDateFreeDiscoveryAndRejectsPartialDateFilters() throws Exception {
        mvc.perform(get("/api/v1/properties?city=Bengaluru")
                        .with(httpBasic("demo", "test-only-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mvc.perform(get("/api/v1/properties?checkIn=2030-01-10")
                        .with(httpBasic("demo", "test-only-password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    void preventsAUserFromModifyingAnotherOwnersProperty() throws Exception {
        String ownerId = idOf(mvc.perform(post("/api/v1/owners")
                .with(httpBasic("demo", "test-only-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"demo\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        mvc.perform(post("/api/v1/owners/" + ownerId + "/properties")
                        .with(user("other").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Blocked Hotel\",\"city\":\"Bengaluru\",\"locality\":\"HSR\",\"starRating\":4,\"amenities\":[\"wifi\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OWNER_REQUIRED"));
    }

    @Test
    void repeatsBookingWithTheSameIdempotencyKeyWithoutCreatingAnotherBooking() throws Exception {
        String ownerId = idOf(mvc.perform(post("/api/v1/owners")
                .with(httpBasic("demo", "test-only-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"demo\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String propertyId = idOf(mvc.perform(post("/api/v1/owners/" + ownerId + "/properties")
                .with(httpBasic("demo", "test-only-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Idempotent Hotel\",\"city\":\"Bengaluru\",\"locality\":\"HSR\",\"starRating\":4,\"amenities\":[\"wifi\"]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String roomId = idOf(mvc.perform(post("/api/v1/properties/" + propertyId + "/room-types")
                .with(httpBasic("demo", "test-only-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Deluxe\",\"capacity\":2,\"pricePerNight\":100,\"inventoryCount\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String payload = "{\"roomTypeId\":\"" + roomId + "\",\"checkIn\":\"2030-03-10\",\"checkOut\":\"2030-03-12\",\"guests\":2}";

        String first = mvc.perform(post("/api/v1/bookings")
                        .with(httpBasic("demo", "test-only-password"))
                        .header("Idempotency-Key", "same-booking")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = mvc.perform(post("/api/v1/bookings")
                        .with(httpBasic("demo", "test-only-password"))
                        .header("Idempotency-Key", "same-booking")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        org.hamcrest.MatcherAssert.assertThat(idOf(first), org.hamcrest.Matchers.equalTo(idOf(second)));
    }

    private String idOf(String json) {
        return json.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*", "$1");
    }
}
