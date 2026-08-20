package com.udea.worldbank.exception.unit;

import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.ExchangeRateApiException;
import com.udea.worldbank.exception.GlobalExceptionHandler;
import com.udea.worldbank.exception.WorldBankApiException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void test_handleNotFound_returns404WithTheExceptionMessage() {
        // Arrange
        DataNotFoundException ex = new DataNotFoundException("no data found");

        // Act
        ProblemDetail result = handler.handleNotFound(ex);

        // Assert
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).isEqualTo("no data found");
    }

    @Test
    void test_handleExternalApiError_returns502WithTheExceptionMessage() {
        // Arrange
        WorldBankApiException ex = new WorldBankApiException("external failure");

        // Act
        ProblemDetail result = handler.handleExternalApiError(ex);

        // Assert
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(result.getDetail()).isEqualTo("external failure");
    }

    @Test
    void test_handleExchangeRateApiError_returns502WithTheExceptionMessage() {
        // Arrange
        ExchangeRateApiException ex = new ExchangeRateApiException("rate provider unavailable");

        // Act
        ProblemDetail result = handler.handleExchangeRateApiError(ex);

        // Assert
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(result.getDetail()).isEqualTo("rate provider unavailable");
    }

    @Test
    void test_handleValidationError_returns400WithTheExceptionMessage() {
        // Arrange
        ConstraintViolationException ex = new ConstraintViolationException("invalid parameter", Set.of());

        // Act
        ProblemDetail result = handler.handleValidationError(ex);

        // Assert
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getDetail()).isEqualTo("invalid parameter");
    }
}
