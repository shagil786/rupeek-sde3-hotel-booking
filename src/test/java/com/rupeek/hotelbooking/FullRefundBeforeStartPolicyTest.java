package com.rupeek.hotelbooking;

import com.rupeek.hotelbooking.domain.FullRefundBeforeStartPolicy;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class FullRefundBeforeStartPolicyTest {
    @Test void refundsBeforeStartOnly() {
        var policy=new FullRefundBeforeStartPolicy(); var amount=BigDecimal.valueOf(100);
        assertThat(policy.refundAmount(amount,LocalDate.of(2030,1,10),LocalDate.of(2030,1,9))).isEqualByComparingTo(amount);
        assertThat(policy.refundAmount(amount,LocalDate.of(2030,1,10),LocalDate.of(2030,1,10))).isZero();
    }
}
