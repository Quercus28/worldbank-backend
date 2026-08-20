package com.udea.worldbank.dto.unit;

import com.udea.worldbank.dto.GdpPerCapitaResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class GdpPerCapitaResponseTest {

    @Test
    void test_accessors_returnValuesUsedInConstruction() {
        // Arrange & Act
        GdpPerCapitaResponse result = new GdpPerCapitaResponse(
                "BRA",
                2022,
                new BigDecimal("1920000000000.00"),
                "$1,920,000,000,000.00",
                215_000_000L,
                new BigDecimal("8930.23"),
                "$8,930.23",
                "EUR",
                new BigDecimal("7644.10"),
                "€7,644.10",
                new BigDecimal("0.85609"),
                "2026-08-20");

        // Assert
        assertThat(result.countryIso3()).isEqualTo("BRA");
        assertThat(result.year()).isEqualTo(2022);
        assertThat(result.gdpUsd()).isEqualByComparingTo("1920000000000.00");
        assertThat(result.gdpUsdFormatted()).isEqualTo("$1,920,000,000,000.00");
        assertThat(result.population()).isEqualTo(215_000_000L);
        assertThat(result.gdpPerCapitaUsd()).isEqualByComparingTo("8930.23");
        assertThat(result.gdpPerCapitaUsdFormatted()).isEqualTo("$8,930.23");
        assertThat(result.targetCurrency()).isEqualTo("EUR");
        assertThat(result.gdpPerCapitaConverted()).isEqualByComparingTo("7644.10");
        assertThat(result.gdpPerCapitaConvertedFormatted()).isEqualTo("€7,644.10");
        assertThat(result.exchangeRate()).isEqualByComparingTo("0.85609");
        assertThat(result.exchangeRateDate()).isEqualTo("2026-08-20");
    }

    @Test
    void test_accessors_allowNullConversionFields_whenNoCurrencyWasRequested() {
        // Arrange & Act
        GdpPerCapitaResponse result = new GdpPerCapitaResponse(
                "BRA", 2022,
                new BigDecimal("1920000000000.00"), "$1,920,000,000,000.00",
                215_000_000L,
                new BigDecimal("8930.23"), "$8,930.23",
                null, null, null, null, null);

        // Assert
        assertThat(result.targetCurrency()).isNull();
        assertThat(result.gdpPerCapitaConverted()).isNull();
        assertThat(result.gdpPerCapitaConvertedFormatted()).isNull();
        assertThat(result.exchangeRate()).isNull();
        assertThat(result.exchangeRateDate()).isNull();
    }
}
