package com.udea.worldbank.controller.unit;

import com.udea.worldbank.controller.IndicatorController;
import com.udea.worldbank.dto.ExchangeRateResponse;
import com.udea.worldbank.dto.GdpPerCapitaResponse;
import com.udea.worldbank.dto.RankingItemResponse;
import com.udea.worldbank.dto.SeriesResponse;
import com.udea.worldbank.service.CalculateGdpPerCapitaUseCase;
import com.udea.worldbank.service.ExchangeRateUseCase;
import com.udea.worldbank.service.GetSeriesUseCase;
import com.udea.worldbank.service.RankingUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class IndicatorControllerTest {

    @Mock
    private GetSeriesUseCase getSeriesUseCase;
    @Mock
    private CalculateGdpPerCapitaUseCase calculateGdpPerCapitaUseCase;
    @Mock
    private RankingUseCase rankingUseCase;
    @Mock
    private ExchangeRateUseCase exchangeRateUseCase;

    @InjectMocks
    private IndicatorController controller;

    @Test
    void test_series_delegatesToTheUseCaseAndReturnsItsResult() {
        // Arrange
        SeriesResponse expected = new SeriesResponse("BRA", "Brazil", "SP.POP.TOTL", "Population, total", List.of());
        when(getSeriesUseCase.getSeries("BRA", "SP.POP.TOTL", 2010, 2020)).thenReturn(expected);

        // Act
        SeriesResponse result = controller.series("BRA", "SP.POP.TOTL", 2010, 2020);

        // Assert
        assertThat(result).isSameAs(expected);
    }

    @Test
    void test_gdpPerCapita_delegatesToTheUseCaseAndReturnsItsResult() {
        // Arrange
        GdpPerCapitaResponse expected = new GdpPerCapitaResponse(
                "BRA", 2022, BigDecimal.ONE, "$1.00", 1L, BigDecimal.ONE, "$1.00",
                "EUR", BigDecimal.ONE, "€1.00", BigDecimal.ONE, "2026-08-20");
        when(calculateGdpPerCapitaUseCase.calculateGdpPerCapita("BRA", 2022, "EUR")).thenReturn(expected);

        // Act
        GdpPerCapitaResponse result = controller.gdpPerCapita("BRA", 2022, "EUR");

        // Assert
        assertThat(result).isSameAs(expected);
    }

    @Test
    void test_ranking_delegatesToTheUseCaseAndReturnsItsResult() {
        // Arrange
        List<RankingItemResponse> expected = List.of(new RankingItemResponse(1, "USA", "United States", 1.0));
        when(rankingUseCase.ranking("NY.GDP.MKTP.CD", 2022, 10)).thenReturn(expected);

        // Act
        List<RankingItemResponse> result = controller.ranking("NY.GDP.MKTP.CD", 2022, 10);

        // Assert
        assertThat(result).isSameAs(expected);
    }

    @Test
    void test_exchangeRate_delegatesToTheUseCaseAndReturnsItsResult() {
        // Arrange
        ExchangeRateResponse expected = new ExchangeRateResponse(
                "USD", "EUR", new BigDecimal("0.85609"), BigDecimal.TEN,
                new BigDecimal("8.56"), "€8.56", "2026-08-20");
        when(exchangeRateUseCase.convert("USD", "EUR", BigDecimal.TEN)).thenReturn(expected);

        // Act
        ExchangeRateResponse result = controller.exchangeRate("USD", "EUR", BigDecimal.TEN);

        // Assert
        assertThat(result).isSameAs(expected);
    }
}
