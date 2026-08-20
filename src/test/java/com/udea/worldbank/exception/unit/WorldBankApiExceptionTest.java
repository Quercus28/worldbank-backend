package com.udea.worldbank.exception.unit;

import com.udea.worldbank.exception.WorldBankApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class WorldBankApiExceptionTest {

    @Test
    void test_constructor_withMessageOnly_setsTheMessageAndHasNoCause() {
        // Arrange & Act
        WorldBankApiException ex = new WorldBankApiException("external failure");

        // Assert
        assertThat(ex.getMessage()).isEqualTo("external failure");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void test_constructor_withMessageAndCause_setsBoth() {
        // Arrange
        Throwable cause = new RuntimeException("timeout");

        // Act
        WorldBankApiException ex = new WorldBankApiException("external failure", cause);

        // Assert
        assertThat(ex.getMessage()).isEqualTo("external failure");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
