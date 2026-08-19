package com.rupeek.hotelbooking;

import com.rupeek.hotelbooking.application.DemandPricingStrategy;
import com.rupeek.hotelbooking.application.StaticPricingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PricingStrategyTest {
    @Test void staticPricingUsesBaseNightlyRate() {
        assertThat(new StaticPricingStrategy().total(new BigDecimal("100"), 2, 1, 0))
                .isEqualByComparingTo("200.00");
    }

    @Test void demandPricingRaisesPriceWithUtilizationAndCapsMarkup() {
        assertThat(new DemandPricingStrategy().total(new BigDecimal("100"), 2, 2, 2))
                .isEqualByComparingTo("300.00");
    }
}
