package com.udea.worldbank.exception.unit;

import com.udea.worldbank.exception.ExchangeRateApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ExchangeRateApiExceptionTest {

    @Test
    void test_constructor_withMessageOnly_setsTheMessageAndHasNoCause() {
        // Arrange & Act
        ExchangeRateApiException ex = new ExchangeRateApiException("external failure");

        // Assert
        assertThat(ex.getMessage()).isEqualTo("external failure");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void test_constructor_withMessageAndCause_setsBoth() {
        // Arrange
        Throwable cause = new RuntimeException("timeout");

        // Act
        ExchangeRateApiException ex = new ExchangeRateApiException("external failure", cause);

        // Assert
        assertThat(ex.getMessage()).isEqualTo("external failure");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
