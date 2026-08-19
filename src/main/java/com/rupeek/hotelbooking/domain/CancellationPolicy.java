package com.rupeek.hotelbooking.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CancellationPolicy {
    BigDecimal refundAmount(BigDecimal bookingAmount, LocalDate startDate, LocalDate cancellationDate);
}
