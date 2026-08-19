package com.rupeek.hotelbooking.application;

import java.math.BigDecimal;

public interface PaymentProvider {
    PaymentResult charge(String bookingId, BigDecimal amount, String method);
    PaymentResult refund(String bookingId, BigDecimal amount);
    record PaymentResult(boolean successful, String reference) {}
}
