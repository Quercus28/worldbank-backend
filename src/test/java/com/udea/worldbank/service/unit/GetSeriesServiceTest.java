package com.udea.worldbank.service.unit;

import com.udea.worldbank.client.ObservationsWorldBankClient;
import com.udea.worldbank.dto.SeriesResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.WorldBankApiException;
import com.udea.worldbank.model.Observation;
import com.udea.worldbank.service.GetSeriesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class GetSeriesServiceTest {

    @Mock
    private ObservationsWorldBankClient observationsClient;

    @InjectMocks
    private GetSeriesService service;

    @Test
    void test_getSeries_returnsPointsFilteredAndSortedChronologically_whenThereAreValidObservations() {
        // Arrange
        List<Observation> observations = List.of(
                new Observation("BR", "Brazil", "BRA", "SP.POP.TOTL", "Population, total", 2020, 210000000.0),
                new Observation("BR", "Brazil", "BRA", "SP.POP.TOTL", "Population, total", 2018, null),
                new Observation("BR", "Brazil", "BRA", "SP.POP.TOTL", "Population, total", 2019, 209000000.0));
        when(observationsClient.getObservations("BRA", "SP.POP.TOTL", 2018, 2020))
                .thenReturn(observations);

        // Act
        SeriesResponse result = service.getSeries("BRA", "SP.POP.TOTL", 2018, 2020);

        // Assert
        assertThat(result.countryIso3()).isEqualTo("BRA");
        assertThat(result.countryName()).isEqualTo("Brazil");
        assertThat(result.indicatorId()).isEqualTo("SP.POP.TOTL");
        assertThat(result.indicatorName()).isEqualTo("Population, total");
        assertThat(result.points()).hasSize(2);
        assertThat(result.points().get(0).year()).isEqualTo(2019);
        assertThat(result.points().get(0).value()).isEqualTo(209000000.0);
        assertThat(result.points().get(1).year()).isEqualTo(2020);
        assertThat(result.points().get(1).value()).isEqualTo(210000000.0);
    }

    @Test
    void test_getSeries_normalizesIso3ToUppercase_whenTheParameterIsLowercase() {
        // Arrange
        List<Observation> observations = List.of(
                new Observation("BR", "Brazil", "BRA", "SP.POP.TOTL", "Population, total", 2020, 210000000.0));
        when(observationsClient.getObservations(anyString(), anyString(), any(), any()))
                .thenReturn(observations);

        // Act
        SeriesResponse result = service.getSeries("bra", "SP.POP.TOTL", null, null);

        // Assert
        assertThat(result.countryIso3()).isEqualTo("BRA");
    }

    @Test
    void test_getSeries_throwsDataNotFoundException_whenAllObservationsHaveANullValue() {
        // Arrange
        List<Observation> observations = List.of(
                new Observation("BR", "Brazil", "BRA", "SP.POP.TOTL", "Population, total", 2020, null));
        when(observationsClient.getObservations("BRA", "SP.POP.TOTL", 2020, 2020))
                .thenReturn(observations);

        // Act
        Throwable thrown = catchThrowable(() -> service.getSeries("BRA", "SP.POP.TOTL", 2020, 2020));

        // Assert
        assertThat(thrown).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("SP.POP.TOTL")
                .hasMessageContaining("BRA");
    }

    @Test
    void test_getSeries_throwsDataNotFoundException_whenTheApiReturnsNoObservations() {
        // Arrange
        when(observationsClient.getObservations("XXX", "SP.POP.TOTL", null, null))
                .thenReturn(List.of());

        // Act
        Throwable thrown = catchThrowable(() -> service.getSeries("XXX", "SP.POP.TOTL", null, null));

        // Assert
        assertThat(thrown).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void test_getSeries_propagatesWorldBankApiException_whenCommunicationWithTheApiFails() {
        // Arrange
        WorldBankApiException failure = new WorldBankApiException("timeout");
        when(observationsClient.getObservations("BRA", "SP.POP.TOTL", null, null))
                .thenThrow(failure);

        // Act
        Throwable thrown = catchThrowable(() -> service.getSeries("BRA", "SP.POP.TOTL", null, null));

        // Assert
        assertThat(thrown).isInstanceOf(WorldBankApiException.class)
                .hasCause(failure)
                .hasMessageContaining(failure.getMessage());
    }
}
