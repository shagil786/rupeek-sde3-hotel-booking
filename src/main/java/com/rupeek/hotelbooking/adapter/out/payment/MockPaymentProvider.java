package com.rupeek.hotelbooking.adapter.out.payment;

import com.rupeek.hotelbooking.application.PaymentProvider;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockPaymentProvider implements PaymentProvider {
    private final ConcurrentHashMap<String, PaymentResult> charges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PaymentResult> refunds = new ConcurrentHashMap<>();
    @Override public PaymentResult charge(String bookingId, BigDecimal amount, String method, String idempotencyKey) { return charges.computeIfAbsent(bookingId + ":" + idempotencyKey, ignored -> "FAIL".equalsIgnoreCase(method) ? new PaymentResult(false, "mock-failure-" + bookingId) : new PaymentResult(true, "mock-charge-" + bookingId)); }
    @Override public PaymentResult refund(String bookingId, BigDecimal amount, String idempotencyKey) { return refunds.computeIfAbsent(bookingId + ":" + idempotencyKey, ignored -> new PaymentResult(true, "mock-refund-" + bookingId)); }
}
