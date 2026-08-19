package com.rupeek.hotelbooking.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@ConditionalOnProperty(name = "app.pricing.strategy", havingValue = "dynamic")
public class DemandPricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal total(BigDecimal nightlyRate, long nights, int inventory, long overlappingBookings) {
        if (inventory < 1) throw new IllegalArgumentException("Inventory must be positive.");
        BigDecimal utilization = BigDecimal.valueOf(overlappingBookings)
                .divide(BigDecimal.valueOf(inventory), 4, RoundingMode.HALF_UP);
        BigDecimal multiplier = BigDecimal.ONE.add(utilization.min(new BigDecimal("0.50")));
        return nightlyRate.multiply(BigDecimal.valueOf(nights)).multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
