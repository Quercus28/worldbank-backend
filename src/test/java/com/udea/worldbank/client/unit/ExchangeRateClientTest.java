package com.udea.worldbank.client.unit;

import com.udea.worldbank.client.ExchangeRateClient;
import com.udea.worldbank.client.dto.FrankfurterLatestRates;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.ExchangeRateApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
@Tag("unit")
class ExchangeRateClientTest {

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec uriSpec;
    @Mock
    private RestClient.RequestHeadersSpec headersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ExchangeRateClient newClient() {
        return new ExchangeRateClient(restClient);
    }

    private void stubChain() {
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void test_getLatestRate_returnsTheResponse_whenTheRequestedCurrencyIsPresent() {
        // Arrange
        stubChain();
        FrankfurterLatestRates response = new FrankfurterLatestRates(
                BigDecimal.ONE, "USD", "2026-08-20", Map.of("EUR", new BigDecimal("0.85609")));
        when(responseSpec.body(FrankfurterLatestRates.class)).thenReturn(response);

        // Act
        FrankfurterLatestRates result = newClient().getLatestRate("USD", "EUR");

        // Assert
        assertThat(result).isSameAs(response);
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_getLatestRate_queriesLatestWithBaseAndSymbolsParameters() {
        // Arrange
        ArgumentCaptor<Function<UriBuilder, URI>> captor = ArgumentCaptor.forClass(Function.class);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(captor.capture())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(FrankfurterLatestRates.class)).thenReturn(
                new FrankfurterLatestRates(BigDecimal.ONE, "USD", "2026-08-20", Map.of("EUR", BigDecimal.ONE)));

        // Act
        newClient().getLatestRate("USD", "EUR");
        URI uri = captor.getValue().apply(new DefaultUriBuilderFactory("https://api.frankfurter.dev/v1").builder());

        // Assert
        assertThat(uri.toString())
                .contains("/latest")
                .contains("base=USD")
                .contains("symbols=EUR");
    }

    @Test
    void test_getLatestRate_throwsDataNotFoundException_whenTheResponseIsNull() {
        // Arrange
        stubChain();
        when(responseSpec.body(FrankfurterLatestRates.class)).thenReturn(null);

        // Act
        Throwable thrown = catchThrowable(() -> newClient().getLatestRate("USD", "EUR"));

        // Assert
        assertThat(thrown).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void test_getLatestRate_throwsDataNotFoundException_whenRatesIsNull() {
        // Arrange
        stubChain();
        when(responseSpec.body(FrankfurterLatestRates.class))
                .thenReturn(new FrankfurterLatestRates(BigDecimal.ONE, "USD", "2026-08-20", null));

        // Act
        Throwable thrown = catchThrowable(() -> newClient().getLatestRate("USD", "EUR"));

        // Assert
        assertThat(thrown).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void test_getLatestRate_throwsDataNotFoundException_whenRatesDoesNotContainTheRequestedCurrency() {
        // Arrange
        stubChain();
        when(responseSpec.body(FrankfurterLatestRates.class)).thenReturn(
                new FrankfurterLatestRates(BigDecimal.ONE, "USD", "2026-08-20", Map.of("GBP", BigDecimal.ONE)));

        // Act
        Throwable thrown = catchThrowable(() -> newClient().getLatestRate("USD", "EUR"));

        // Assert
        assertThat(thrown).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void test_getLatestRate_throwsDataNotFoundException_whenTheApiRespondsWithAClientError() {
        // Arrange
        stubChain();
        when(responseSpec.body(FrankfurterLatestRates.class)).thenThrow(
                HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        // Act
        Throwable thrown = catchThrowable(() -> newClient().getLatestRate("USD", "ZZZ"));

        // Assert
        assertThat(thrown).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("ZZZ");
    }

    @Test
    void test_getLatestRate_throwsExchangeRateApiException_whenTheHttpCallFails() {
        // Arrange
        stubChain();
        when(responseSpec.body(FrankfurterLatestRates.class))
                .thenThrow(new ResourceAccessException("timeout"));

        // Act
        Throwable thrown = catchThrowable(() -> newClient().getLatestRate("USD", "EUR"));

        // Assert
        assertThat(thrown).isInstanceOf(ExchangeRateApiException.class)
                .hasMessageContaining("timeout")
                .hasCauseInstanceOf(ResourceAccessException.class);
    }
}
