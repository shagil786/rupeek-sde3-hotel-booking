package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.adapter.in.web.ApiException;
import com.rupeek.hotelbooking.adapter.in.web.HotelDtos;
import com.rupeek.hotelbooking.adapter.out.persistence.*;
import com.rupeek.hotelbooking.domain.*;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class HotelBookingService {
    private final OwnerJpaRepository owners; private final PropertyJpaRepository properties; private final RoomTypeJpaRepository rooms;
    private final BookingJpaRepository bookings; private final PaymentJpaRepository payments; private final PaymentProvider paymentProvider;
    private final CancellationPolicy cancellationPolicy = new FullRefundBeforeStartPolicy();

    public HotelBookingService(OwnerJpaRepository owners, PropertyJpaRepository properties, RoomTypeJpaRepository rooms, BookingJpaRepository bookings, PaymentJpaRepository payments, PaymentProvider paymentProvider) {
        this.owners=owners; this.properties=properties; this.rooms=rooms; this.bookings=bookings; this.payments=payments; this.paymentProvider=paymentProvider;
    }

    @Transactional
    public HotelDtos.ResourceResponse createOwner(HotelDtos.CreateOwnerRequest request, String actor) {
        if (!actor.equals(request.username())) throw ApiException.forbidden("OWNER_ACTOR_MISMATCH","The authenticated user must create their own owner account.");
        OwnerEntity owner = owners.save(new OwnerEntity(UUID.randomUUID().toString(), request.username()));
        return new HotelDtos.ResourceResponse(owner.getId());
    }

    @Transactional
    public HotelDtos.ResourceResponse createProperty(String ownerId, HotelDtos.CreatePropertyRequest request, String actor) {
        OwnerEntity owner=owners.findById(ownerId).orElseThrow(()->ApiException.notFound("OWNER_NOT_FOUND","Owner not found."));
        requireOwner(owner, actor);
        String amenities=String.join(",", request.amenities()==null?List.of():request.amenities());
        PropertyEntity property=properties.save(new PropertyEntity(UUID.randomUUID().toString(),owner,request.name(),request.city(),request.locality(),request.starRating(),amenities));
        return new HotelDtos.ResourceResponse(property.getId());
    }

    @Transactional
    public HotelDtos.ResourceResponse createRoomType(String propertyId, HotelDtos.CreateRoomTypeRequest request, String actor) {
        PropertyEntity property=properties.findById(propertyId).orElseThrow(()->ApiException.notFound("PROPERTY_NOT_FOUND","Property not found."));
        requireOwner(property.getOwner(),actor);
        RoomTypeEntity room=rooms.save(new RoomTypeEntity(UUID.randomUUID().toString(),property,request.name(),request.capacity(),request.pricePerNight(),request.inventoryCount()));
        return new HotelDtos.ResourceResponse(room.getId());
    }

    @Transactional
    public SearchPage<HotelDtos.PropertySearchResponse> search(String city, String locality, LocalDate checkIn, LocalDate checkOut, Integer guests, BigDecimal minPrice, BigDecimal maxPrice, Integer stars, String amenities, int page, int size) {
        validatePage(page,size);
        validateOptionalDates(checkIn,checkOut);
        List<HotelDtos.PropertySearchResponse> all=properties.findAll().stream().filter(p->city==null||p.getCity().equalsIgnoreCase(city)).filter(p->locality==null||p.getLocality().equalsIgnoreCase(locality)).filter(p->stars==null||p.getStarRating()>=stars).filter(p->hasAmenities(p,amenities)).map(p->{
            List<HotelDtos.RoomAvailability> available=rooms.findByPropertyId(p.getId()).stream().filter(r->guests==null||r.getCapacity()>=guests).filter(r->minPrice==null||r.getPricePerNight().compareTo(minPrice)>=0).filter(r->maxPrice==null||r.getPricePerNight().compareTo(maxPrice)<=0).map(r->toAvailability(r,checkIn,checkOut)).filter(Objects::nonNull).toList();
            return available.isEmpty()?null:new HotelDtos.PropertySearchResponse(p.getId(),p.getName(),p.getCity(),p.getLocality(),p.getStarRating(),split(p.getAmenities()),available);
        }).filter(Objects::nonNull).toList();
        int from=(int)Math.min((long)page*size,all.size()); return new SearchPage<>(all.subList(from,Math.min(from+size,all.size())),all.size());
    }

    @Transactional
    public HotelDtos.BookingResponse createBooking(HotelDtos.CreateBookingRequest request, String actor, String idempotencyKey) {
        if (idempotencyKey==null||idempotencyKey.isBlank()) throw ApiException.badRequest("IDEMPOTENCY_KEY_REQUIRED","Idempotency-Key is required.");
        var previous=bookings.findByCustomerUsernameAndIdempotencyKey(actor,idempotencyKey); if(previous.isPresent()) return toResponse(previous.get());
        validateDates(request.checkIn(),request.checkOut());
        RoomTypeEntity room=rooms.findById(request.roomTypeId()).orElseThrow(()->ApiException.notFound("ROOM_TYPE_NOT_FOUND","Room type not found."));
        if(room.getCapacity()<request.guests()) throw ApiException.badRequest("GUEST_CAPACITY_EXCEEDED","Room capacity is too small.");
        if(!available(room,request.checkIn(),request.checkOut())) throw ApiException.conflict("ROOM_UNAVAILABLE","The room inventory is unavailable for those dates.");
        long nights=ChronoUnit.DAYS.between(request.checkIn(),request.checkOut());
        room.markBookingMutation();
        rooms.save(room);
        BookingEntity booking=bookings.save(new BookingEntity(UUID.randomUUID().toString(),room,actor,request.checkIn(),request.checkOut(),request.guests(),room.getPricePerNight().multiply(BigDecimal.valueOf(nights)),idempotencyKey));
        return toResponse(booking);
    }

    @Transactional
    public HotelDtos.BookingResponse pay(String bookingId, HotelDtos.PaymentRequest request, String actor, String idempotencyKey) {
        requireKey(idempotencyKey); BookingEntity booking=findBooking(bookingId); requireCustomer(booking,actor);
        if(payments.findByBookingIdAndIdempotencyKey(bookingId,idempotencyKey).isPresent()) return toResponse(booking);
        if(booking.getStatus()==BookingStatus.CONFIRMED) return toResponse(booking);
        if(booking.getStatus()!=BookingStatus.PENDING_PAYMENT) throw ApiException.conflict("INVALID_BOOKING_STATE","Only pending bookings can be paid.");
        if(!available(booking.getRoomType(),booking.getCheckIn(),booking.getCheckOut(),booking.getId())) throw ApiException.conflict("ROOM_UNAVAILABLE","The room inventory is no longer available.");
        var result=paymentProvider.charge(bookingId,booking.getAmount(),request.method());
        payments.save(new PaymentEntity(UUID.randomUUID().toString(),booking,request.method(),booking.getAmount(),result.successful()?PaymentStatus.SUCCEEDED:PaymentStatus.FAILED,idempotencyKey));
        booking.setStatus(result.successful()?BookingStatus.CONFIRMED:BookingStatus.PAYMENT_FAILED); bookings.save(booking); return toResponse(booking);
    }

    @Transactional
    public void cancel(String bookingId, String actor, String idempotencyKey) {
        requireKey(idempotencyKey); BookingEntity booking=findBooking(bookingId); requireCustomer(booking,actor);
        if(idempotencyKey.equals(booking.getCancellationIdempotencyKey())||booking.getStatus()==BookingStatus.CANCELLED) return;
        if(booking.getStatus()!=BookingStatus.CONFIRMED) throw ApiException.conflict("INVALID_BOOKING_STATE","Only confirmed bookings can be cancelled.");
        BigDecimal refund=cancellationPolicy.refundAmount(booking.getAmount(),booking.getCheckIn(),LocalDate.now());
        if(refund.signum()>0) paymentProvider.refund(bookingId,refund);
        booking.setStatus(BookingStatus.CANCELLED); booking.setCancellationIdempotencyKey(idempotencyKey); bookings.save(booking);
    }

    @Transactional
    public HotelDtos.BookingResponse getBooking(String bookingId,String actor) { BookingEntity b=findBooking(bookingId); requireCustomer(b,actor); return toResponse(b); }

    @Transactional
    public int expireAbandonedBookings(java.time.Instant now, long holdMinutes) {
        var expired = bookings.findByStatusAndCreatedAtBefore(BookingStatus.PENDING_PAYMENT, now.minus(holdMinutes, ChronoUnit.MINUTES));
        expired.forEach(b -> { b.setStatus(BookingStatus.EXPIRED); bookings.save(b); });
        return expired.size();
    }

    @Scheduled(fixedDelayString = "${app.booking.expiration-check-ms:60000}")
    @Transactional
    public void expireAbandonedBookings() {
        expireAbandonedBookings(java.time.Instant.now(), holdMinutes);
    }

    private HotelDtos.RoomAvailability toAvailability(RoomTypeEntity room,LocalDate in,LocalDate out){
        if(in==null||out==null) return new HotelDtos.RoomAvailability(room.getId(),room.getProperty().getId(),room.getProperty().getName(),room.getName(),room.getPricePerNight(),room.getCapacity(),room.getInventoryCount());
        int used=bookings.findByRoomTypeIdAndStatusInAndCheckInLessThanAndCheckOutGreaterThan(room.getId(),List.of(BookingStatus.PENDING_PAYMENT,BookingStatus.CONFIRMED),out,in).size();
        return used>=room.getInventoryCount()?null:new HotelDtos.RoomAvailability(room.getId(),room.getProperty().getId(),room.getProperty().getName(),room.getName(),room.getPricePerNight(),room.getCapacity(),room.getInventoryCount()-used);
    }
    private boolean available(RoomTypeEntity r,LocalDate in,LocalDate out){return available(r,in,out,null);}
    private boolean available(RoomTypeEntity r,LocalDate in,LocalDate out,String excluding){int used=bookings.findByRoomTypeIdAndStatusInAndCheckInLessThanAndCheckOutGreaterThan(r.getId(),List.of(BookingStatus.PENDING_PAYMENT,BookingStatus.CONFIRMED),out,in).stream().filter(b->!Objects.equals(b.getId(),excluding)).toList().size(); return used<r.getInventoryCount();}
    private boolean hasAmenities(PropertyEntity p,String requested){if(requested==null||requested.isBlank())return true; Set<String> have=new HashSet<>(split(p.getAmenities())); return Arrays.stream(requested.split(",")).map(String::trim).allMatch(have::contains);}
    private List<String> split(String s){return s==null||s.isBlank()?List.of():Arrays.stream(s.split(",")).map(String::trim).filter(x->!x.isBlank()).toList();}
    private void validateDates(LocalDate in,LocalDate out){if(in==null||out==null||!out.isAfter(in))throw ApiException.badRequest("INVALID_DATE_RANGE","Check-out must be after check-in.");}
    private void validateOptionalDates(LocalDate in,LocalDate out){if((in==null)!=(out==null))throw ApiException.badRequest("INVALID_DATE_RANGE","Check-in and check-out must be provided together.");if(in!=null)validateDates(in,out);}
    private BookingEntity findBooking(String id){return bookings.findById(id).orElseThrow(()->ApiException.notFound("BOOKING_NOT_FOUND","Booking not found."));}
    private void requireOwner(OwnerEntity owner,String actor){if(!owner.getUsername().equals(actor))throw ApiException.forbidden("OWNER_REQUIRED","Only the owner can modify this resource.");}
    private void requireCustomer(BookingEntity booking,String actor){if(!booking.getCustomerUsername().equals(actor))throw ApiException.forbidden("BOOKING_OWNER_REQUIRED","Only the booking owner can access it.");}
    private void requireKey(String key){if(key==null||key.isBlank())throw ApiException.badRequest("IDEMPOTENCY_KEY_REQUIRED","Idempotency-Key is required.");}
    private void validatePage(int page,int size){if(page<0||size<1||size>100)throw ApiException.badRequest("INVALID_PAGINATION","page must be non-negative and size must be between 1 and 100.");}
    @org.springframework.beans.factory.annotation.Value("${app.booking.hold-minutes:15}")
    private long holdMinutes;
    public record SearchPage<T>(List<T> items,int total) {}
    private HotelDtos.BookingResponse toResponse(BookingEntity b){return new HotelDtos.BookingResponse(b.getId(),b.getRoomType().getId(),b.getCustomerUsername(),b.getCheckIn(),b.getCheckOut(),b.getGuests(),b.getAmount(),b.getStatus().name());}
}
