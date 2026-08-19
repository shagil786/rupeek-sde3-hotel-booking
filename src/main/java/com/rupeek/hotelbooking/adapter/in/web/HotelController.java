package com.rupeek.hotelbooking.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static com.rupeek.hotelbooking.adapter.in.web.HotelDtos.*;

@RestController
@RequestMapping("/api/v1")
public class HotelController {
    private final com.rupeek.hotelbooking.application.HotelBookingService service;
    public HotelController(com.rupeek.hotelbooking.application.HotelBookingService service){this.service=service;}

    @PostMapping("/owners")
    public ResponseEntity<ResourceResponse> owner(@Valid @RequestBody CreateOwnerRequest request, Authentication auth){var r=service.createOwner(request,auth.getName());return ResponseEntity.created(URI.create("/api/v1/owners/"+r.id())).body(r);}
    @PostMapping("/owners/{ownerId}/properties")
    public ResponseEntity<ResourceResponse> property(@PathVariable String ownerId,@Valid @RequestBody CreatePropertyRequest request,Authentication auth){var r=service.createProperty(ownerId,request,auth.getName());return ResponseEntity.created(URI.create("/api/v1/properties/"+r.id())).body(r);}
    @PostMapping("/properties/{propertyId}/room-types")
    public ResponseEntity<ResourceResponse> room(@PathVariable String propertyId,@Valid @RequestBody CreateRoomTypeRequest request,Authentication auth){var r=service.createRoomType(propertyId,request,auth.getName());return ResponseEntity.created(URI.create("/api/v1/room-types/"+r.id())).body(r);}
    @GetMapping("/properties")
    public ResponseEntity<List<PropertySearchResponse>> search(@RequestParam(required=false) String city,@RequestParam(required=false) String locality,@RequestParam(required=false) LocalDate checkIn,@RequestParam(required=false) LocalDate checkOut,@RequestParam(required=false) Integer guests,@RequestParam(required=false) BigDecimal minPrice,@RequestParam(required=false) BigDecimal maxPrice,@RequestParam(required=false) Integer stars,@RequestParam(required=false) String amenities,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){var result=service.search(city,locality,checkIn,checkOut,guests,minPrice,maxPrice,stars,amenities,page,size); long end=(long)page*size+result.items().size(); return ResponseEntity.ok().header("X-Page",String.valueOf(page)).header("X-Page-Size",String.valueOf(size)).header("X-Total-Count",String.valueOf(result.total())).header("X-Has-More",String.valueOf(end<result.total())).body(result.items());}
    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> book(@Valid @RequestBody CreateBookingRequest request,@RequestHeader("Idempotency-Key") String key,Authentication auth){var r=service.createBooking(request,auth.getName(),key);return ResponseEntity.created(URI.create("/api/v1/bookings/"+r.id())).body(r);}
    @PostMapping("/bookings/{bookingId}/payments")
    public BookingResponse pay(@PathVariable String bookingId,@Valid @RequestBody PaymentRequest request,@RequestHeader("Idempotency-Key") String key,Authentication auth){return service.pay(bookingId,request,auth.getName(),key);}
    @PostMapping("/bookings/{bookingId}/cancellations")
    public ResponseEntity<Void> cancel(@PathVariable String bookingId,@RequestHeader("Idempotency-Key") String key,Authentication auth){service.cancel(bookingId,auth.getName(),key);return ResponseEntity.noContent().build();}
    @GetMapping("/bookings/{bookingId}")
    public BookingResponse get(@PathVariable String bookingId,Authentication auth){return service.getBooking(bookingId,auth.getName());}
}
