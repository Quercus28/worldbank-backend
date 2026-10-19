package com.udea.worldbank.service.unit;

import com.udea.worldbank.client.ExchangeRateClient;
import com.udea.worldbank.client.dto.FrankfurterLatestRates;
import com.udea.worldbank.dto.ExchangeRateResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.ExchangeRateApiException;
import com.udea.worldbank.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @InjectMocks
    private ExchangeRateService service;

    @Test
    void test_convert_returnsTheConvertedAmount_whenTheCurrenciesDiffer() {
        // Arrange
        when(exchangeRateClient.getLatestRate("USD", "EUR")).thenReturn(new FrankfurterLatestRates(
                BigDecimal.ONE, "USD", "2026-08-20", Map.of("EUR", new BigDecimal("0.85609"))));

        // Act
        ExchangeRateResponse result = service.convert("usd", "eur", BigDecimal.TEN);

        // Assert
        assertThat(result.from()).isEqualTo("USD");
        assertThat(result.to()).isEqualTo("EUR");
        assertThat(result.rate()).isEqualByComparingTo("0.85609");
        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(result.convertedAmount()).isEqualByComparingTo("8.56");
        assertThat(result.convertedAmountFormatted()).isEqualTo("€8.56");
        assertThat(result.rateDate()).isEqualTo("2026-08-20");
    }

    @Test
    void test_convert_skipsTheExternalCall_whenBothCurrenciesAreTheSame() {
        // Act
        ExchangeRateResponse result = service.convert("usd", "USD", BigDecimal.TEN);

        // Assert
        assertThat(result.from()).isEqualTo("USD");
        assertThat(result.to()).isEqualTo("USD");
        assertThat(result.rate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.convertedAmount()).isEqualByComparingTo("10.00");
        assertThat(result.convertedAmountFormatted()).isEqualTo("$10.00");
        assertThat(result.rateDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC).toString());
        verify(exchangeRateClient, never()).getLatestRate("USD", "USD");
    }

    @Test
    void test_convert_propagatesDataNotFoundException_whenTheCurrencyPairIsUnsupported() {
        // Arrange
        DataNotFoundException failure = new DataNotFoundException("unsupported currency");
        when(exchangeRateClient.getLatestRate("USD", "ZZZ")).thenThrow(failure);

        // Act
        Throwable thrown = catchThrowable(() -> service.convert("USD", "ZZZ", BigDecimal.ONE));

        // Assert
        assertThat(thrown).isSameAs(failure);
    }

    @Test
    void test_convert_propagatesExchangeRateApiException_whenTheRateProviderFails() {
        // Arrange
        ExchangeRateApiException failure = new ExchangeRateApiException("service unavailable");
        when(exchangeRateClient.getLatestRate("USD", "EUR")).thenThrow(failure);

        // Act
        Throwable thrown = catchThrowable(() -> service.convert("USD", "EUR", BigDecimal.ONE));

        // Assert
        assertThat(thrown).isInstanceOf(ExchangeRateApiException.class)
                .hasCause(failure)
                .hasMessageContaining(failure.getMessage());
    }
}
