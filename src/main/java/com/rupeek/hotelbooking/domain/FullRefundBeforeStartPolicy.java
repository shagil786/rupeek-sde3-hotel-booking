package com.rupeek.hotelbooking.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class FullRefundBeforeStartPolicy implements CancellationPolicy {
    @Override
    public BigDecimal refundAmount(BigDecimal bookingAmount, LocalDate startDate, LocalDate cancellationDate) {
        return cancellationDate.isBefore(startDate) ? bookingAmount : BigDecimal.ZERO;
    }
}
