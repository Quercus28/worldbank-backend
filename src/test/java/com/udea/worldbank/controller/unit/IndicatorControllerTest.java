package com.udea.worldbank.controller.unit;

import com.udea.worldbank.controller.IndicatorController;
import com.udea.worldbank.dto.GdpPerCapitaResponse;
import com.udea.worldbank.dto.RankingItemResponse;
import com.udea.worldbank.dto.SeriesResponse;
import com.udea.worldbank.service.CalculateGdpPerCapitaUseCase;
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
                "BRA", 2022, BigDecimal.ONE, "$1.00", 1L, BigDecimal.ONE, "$1.00");
        when(calculateGdpPerCapitaUseCase.calculateGdpPerCapita("BRA", 2022)).thenReturn(expected);

        // Act
        GdpPerCapitaResponse result = controller.gdpPerCapita("BRA", 2022);

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
}
