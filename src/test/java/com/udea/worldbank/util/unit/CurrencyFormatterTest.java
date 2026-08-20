package com.udea.worldbank.util.unit;

import com.udea.worldbank.util.CurrencyFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class CurrencyFormatterTest {

    @Test
    void test_round_usesTwoDecimals_forACurrencyWithTwoFractionDigits() {
        // Act
        BigDecimal result = CurrencyFormatter.round(new BigDecimal("8930.2345"), "EUR");

        // Assert
        assertThat(result).isEqualByComparingTo("8930.23");
        assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    void test_round_usesZeroDecimals_forACurrencyWithoutFractionDigits() {
        // Act
        BigDecimal result = CurrencyFormatter.round(new BigDecimal("1234.6"), "JPY");

        // Assert
        assertThat(result).isEqualByComparingTo("1235");
        assertThat(result.scale()).isEqualTo(0);
    }

    @Test
    void test_round_fallsBackToTwoDecimals_forACurrencyWithoutADefinedFractionDigitCount() {
        // Act: XAU (onza troy de oro) no tiene decimales por defecto definidos (-1)
        BigDecimal result = CurrencyFormatter.round(new BigDecimal("1.005"), "XAU");

        // Assert
        assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    void test_format_rendersTheAmountWithTheCurrencySymbol() {
        // Act
        String result = CurrencyFormatter.format(new BigDecimal("8930.23"), "USD");

        // Assert
        assertThat(result).isEqualTo("$8,930.23");
    }
}
