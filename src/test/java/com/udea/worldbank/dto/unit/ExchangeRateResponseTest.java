package com.udea.worldbank.dto.unit;

import com.udea.worldbank.dto.ExchangeRateResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ExchangeRateResponseTest {

    @Test
    void test_accessors_returnValuesUsedInConstruction() {
        // Arrange & Act
        ExchangeRateResponse result = new ExchangeRateResponse(
                "USD", "EUR", new BigDecimal("0.85609"), BigDecimal.TEN,
                new BigDecimal("8.56"), "€8.56", "2026-08-20");

        // Assert
        assertThat(result.from()).isEqualTo("USD");
        assertThat(result.to()).isEqualTo("EUR");
        assertThat(result.rate()).isEqualByComparingTo("0.85609");
        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(result.convertedAmount()).isEqualByComparingTo("8.56");
        assertThat(result.convertedAmountFormatted()).isEqualTo("€8.56");
        assertThat(result.rateDate()).isEqualTo("2026-08-20");
    }
}
