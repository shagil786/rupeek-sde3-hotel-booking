package com.rupeek.hotelbooking.adapter.in.web;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class HotelDtos {
    private HotelDtos() {}
    public record CreateOwnerRequest(@NotBlank @Size(max=120) String username) {}
    public record CreatePropertyRequest(@NotBlank @Size(max=200) String name, @NotBlank @Size(max=100) String city, @NotBlank @Size(max=150) String locality, @Min(1) @Max(5) int starRating, List<@NotBlank String> amenities) {}
    public record CreateRoomTypeRequest(@NotBlank @Size(max=120) String name, @Min(1) int capacity, @DecimalMin("0.01") BigDecimal pricePerNight, @Min(1) int inventoryCount) {}
    public record CreateBookingRequest(@NotBlank String roomTypeId, @NotNull @FutureOrPresent LocalDate checkIn, @NotNull LocalDate checkOut, @Min(1) int guests) {}
    public record PaymentRequest(@NotBlank String method) {}
    public record ResourceResponse(String id) {}
    public record BookingResponse(String id, String roomTypeId, String customerUsername, LocalDate checkIn, LocalDate checkOut, int guests, BigDecimal amount, String status) {}
    public record RoomAvailability(String roomTypeId, String propertyId, String propertyName, String roomName, BigDecimal pricePerNight, int capacity, int availableUnits) {}
    public record PropertySearchResponse(String propertyId, String name, String city, String locality, int starRating, List<String> amenities, List<RoomAvailability> rooms) {}
}
