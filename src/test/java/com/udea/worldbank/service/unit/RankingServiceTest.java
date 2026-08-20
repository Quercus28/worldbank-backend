package com.udea.worldbank.service.unit;

import com.udea.worldbank.client.CountriesWorldBankClient;
import com.udea.worldbank.client.ObservationsWorldBankClient;
import com.udea.worldbank.dto.RankingItemResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.WorldBankApiException;
import com.udea.worldbank.model.Observation;
import com.udea.worldbank.service.RankingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RankingServiceTest {

    private static final String INDICATOR = "NY.GDP.MKTP.CD";

    @Mock
    private ObservationsWorldBankClient observationsClient;

    @Mock
    private CountriesWorldBankClient countriesClient;

    @InjectMocks
    private RankingService service;

    @Test
    void test_ranking_returnsCountriesSortedDescendingExcludingAggregatesAndNulls_whenThereIsValidData() {
        // Arrange
        List<Observation> observations = List.of(
                new Observation("US", "United States", "USA", INDICATOR, "GDP", 2022, 25_000_000_000_000.0),
                new Observation("CN", "China", "CHN", INDICATOR, "GDP", 2022, 18_000_000_000_000.0),
                new Observation("WL", "World", "WLD", INDICATOR, "GDP", 2022, 100_000_000_000_000.0),
                new Observation("BR", "Brazil", "BRA", INDICATOR, "GDP", 2022, null),
                new Observation("JP", "Japan", "JPN", INDICATOR, "GDP", 2022, 4_000_000_000_000.0));
        when(observationsClient.getObservations("all", INDICATOR, 2022, 2022))
                .thenReturn(observations);
        when(countriesClient.getRealCountryCodes())
                .thenReturn(Set.of("USA", "CHN", "BRA", "JPN"));

        // Act
        List<RankingItemResponse> result = service.ranking(INDICATOR, 2022, 10);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).extracting(RankingItemResponse::countryIso3)
                .containsExactly("USA", "CHN", "JPN");
        assertThat(result).extracting(RankingItemResponse::position)
                .containsExactly(1, 2, 3);
    }

    @Test
    void test_ranking_truncatesToTheLimit_whenThereAreMoreCountriesThanTheRequestedLimit() {
        // Arrange
        List<Observation> observations = List.of(
                new Observation("US", "United States", "USA", INDICATOR, "GDP", 2022, 25_000_000_000_000.0),
                new Observation("CN", "China", "CHN", INDICATOR, "GDP", 2022, 18_000_000_000_000.0),
                new Observation("JP", "Japan", "JPN", INDICATOR, "GDP", 2022, 4_000_000_000_000.0));
        when(observationsClient.getObservations("all", INDICATOR, 2022, 2022))
                .thenReturn(observations);
        when(countriesClient.getRealCountryCodes())
                .thenReturn(Set.of("USA", "CHN", "JPN"));

        // Act
        List<RankingItemResponse> result = service.ranking(INDICATOR, 2022, 2);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(RankingItemResponse::countryIso3)
                .containsExactly("USA", "CHN");
    }

    @Test
    void test_ranking_throwsDataNotFoundException_whenNoRealCountriesWithValueRemain() {
        // Arrange
        List<Observation> observations = List.of(
                new Observation("WL", "World", "WLD", INDICATOR, "GDP", 2022, 100_000_000_000_000.0),
                new Observation("BR", "Brazil", "BRA", INDICATOR, "GDP", 2022, null));
        when(observationsClient.getObservations("all", INDICATOR, 2022, 2022))
                .thenReturn(observations);
        when(countriesClient.getRealCountryCodes())
                .thenReturn(Set.of("BRA"));

        // Act
        Throwable thrown = catchThrowable(() -> service.ranking(INDICATOR, 2022, 10));

        // Assert
        assertThat(thrown).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining(INDICATOR)
                .hasMessageContaining("2022");
    }

    @Test
    void test_ranking_propagatesWorldBankApiException_whenFetchingObservationsFails() {
        // Arrange
        WorldBankApiException failure = new WorldBankApiException("timeout");
        when(observationsClient.getObservations("all", INDICATOR, 2022, 2022))
                .thenThrow(failure);

        // Act
        Throwable thrown = catchThrowable(() -> service.ranking(INDICATOR, 2022, 10));

        // Assert
        assertThat(thrown).isSameAs(failure);
    }

    @Test
    void test_ranking_propagatesWorldBankApiException_whenFetchingRealCountriesFails() {
        // Arrange
        WorldBankApiException failure = new WorldBankApiException("service unavailable");
        when(observationsClient.getObservations("all", INDICATOR, 2022, 2022))
                .thenReturn(List.of(new Observation("US", "United States", "USA", INDICATOR, "GDP", 2022, 1.0)));
        when(countriesClient.getRealCountryCodes()).thenThrow(failure);

        // Act
        Throwable thrown = catchThrowable(() -> service.ranking(INDICATOR, 2022, 10));

        // Assert
        assertThat(thrown).isSameAs(failure);
    }
}
