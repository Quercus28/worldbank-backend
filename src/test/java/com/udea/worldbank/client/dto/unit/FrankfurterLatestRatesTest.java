package com.udea.worldbank.client.dto.unit;

import com.udea.worldbank.client.dto.FrankfurterLatestRates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class FrankfurterLatestRatesTest {

    @Test
    void test_accessors_returnValuesUsedInConstruction() {
        // Arrange
        Map<String, BigDecimal> rates = Map.of("EUR", new BigDecimal("0.85609"));

        // Act
        FrankfurterLatestRates result = new FrankfurterLatestRates(BigDecimal.ONE, "USD", "2026-08-20", rates);

        // Assert
        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.base()).isEqualTo("USD");
        assertThat(result.date()).isEqualTo("2026-08-20");
        assertThat(result.rates()).isEqualTo(rates);
    }
}
