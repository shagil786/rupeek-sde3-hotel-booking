package com.rupeek.hotelbooking.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@ConditionalOnProperty(name = "app.pricing.strategy", havingValue = "static", matchIfMissing = true)
public class StaticPricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal total(BigDecimal nightlyRate, long nights, int inventory, long overlappingBookings) {
        return nightlyRate.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
    }
}
