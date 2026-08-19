package com.rupeek.hotelbooking.application;

import java.math.BigDecimal;

public interface PaymentProvider {
    PaymentResult charge(String bookingId, BigDecimal amount, String method, String idempotencyKey);
    PaymentResult refund(String bookingId, BigDecimal amount, String idempotencyKey);
    record PaymentResult(boolean successful, String reference) {}
}
