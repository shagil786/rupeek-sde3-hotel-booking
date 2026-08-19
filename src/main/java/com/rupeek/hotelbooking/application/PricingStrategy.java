package com.rupeek.hotelbooking.application;

import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal total(BigDecimal nightlyRate, long nights, int inventory, long overlappingBookings);
}
