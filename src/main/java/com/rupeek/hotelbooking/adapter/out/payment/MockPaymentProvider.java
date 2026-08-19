package com.rupeek.hotelbooking.adapter.out.payment;

import com.rupeek.hotelbooking.application.PaymentProvider;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class MockPaymentProvider implements PaymentProvider {
    @Override public PaymentResult charge(String bookingId, BigDecimal amount, String method) { return "FAIL".equalsIgnoreCase(method) ? new PaymentResult(false, "mock-failure-" + bookingId) : new PaymentResult(true, "mock-charge-" + bookingId); }
    @Override public PaymentResult refund(String bookingId, BigDecimal amount) { return new PaymentResult(true, "mock-refund-" + bookingId); }
}
