package com.udea.worldbank.service.unit;

import com.udea.worldbank.client.ObservationsWorldBankClient;
import com.udea.worldbank.dto.GdpPerCapitaResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.WorldBankApiException;
import com.udea.worldbank.model.Observation;
import com.udea.worldbank.service.CalculateGdpPerCapitaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CalculateGdpPerCapitaServiceTest {

    private static final String GDP_INDICATOR = "NY.GDP.MKTP.CD";
    private static final String POPULATION_INDICATOR = "SP.POP.TOTL";

    @Mock
    private ObservationsWorldBankClient observationsClient;

    @InjectMocks
    private CalculateGdpPerCapitaService service;

    @Test
    void test_calculateGdpPerCapita_returnsARoundedValueAndACurrencyFormattedString_whenGdpAndPopulationExistForTheYear() {
        // Arrange
        when(observationsClient.getObservations("BRA", GDP_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observation(GDP_INDICATOR, 2022, 1_920_000_000_000.0)));
        when(observationsClient.getObservations("BRA", POPULATION_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observation(POPULATION_INDICATOR, 2022, 215_000_000.0)));

        // Act
        GdpPerCapitaResponse result = service.calculateGdpPerCapita("BRA", 2022);

        // Assert
        assertThat(result.countryIso3()).isEqualTo("BRA");
        assertThat(result.year()).isEqualTo(2022);
        assertThat(result.gdpUsd()).isEqualByComparingTo("1920000000000.00");
        assertThat(result.gdpUsdFormatted()).isEqualTo("$1,920,000,000,000.00");
        assertThat(result.population()).isEqualTo(215_000_000L);
        assertThat(result.gdpPerCapitaUsd()).isEqualByComparingTo("8930.23");
        assertThat(result.gdpPerCapitaUsdFormatted()).isEqualTo("$8,930.23");
    }

    @Test
    void test_calculateGdpPerCapita_normalizesIso3ToUppercase_whenTheParameterIsLowercase() {
        // Arrange
        when(observationsClient.getObservations("bra", GDP_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observation(GDP_INDICATOR, 2022, 1000.0)));
        when(observationsClient.getObservations("bra", POPULATION_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observation(POPULATION_INDICATOR, 2022, 10.0)));

        // Act
        GdpPerCapitaResponse result = service.calculateGdpPerCapita("bra", 2022);

        // Assert
        assertThat(result.countryIso3()).isEqualTo("BRA");
    }

    @Test
    void test_calculateGdpPerCapita_discardsObservationsWithoutValue_andUsesTheFirstWithData() {
        // Arrange
        when(observationsClient.getObservations("BRA", GDP_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observationWithoutValue(GDP_INDICATOR, 2022), observation(GDP_INDICATOR, 2022, 1000.0)));
        when(observationsClient.getObservations("BRA", POPULATION_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observation(POPULATION_INDICATOR, 2022, 10.0)));

        // Act
        GdpPerCapitaResponse result = service.calculateGdpPerCapita("BRA", 2022);

        // Assert
        assertThat(result.gdpUsd()).isEqualByComparingTo("1000.00");
        assertThat(result.gdpPerCapitaUsd()).isEqualByComparingTo("100.00");
    }

    @Test
    void test_calculateGdpPerCapita_throwsDataNotFoundException_whenThereIsNoGdpData() {
        // Arrange
        when(observationsClient.getObservations("BRA", GDP_INDICATOR, 2022, 2022))
                .thenReturn(List.of());
        when(observationsClient.getObservations("BRA", POPULATION_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observation(POPULATION_INDICATOR, 2022, 215_000_000.0)));

        // Act
        Throwable thrown = catchThrowable(() -> service.calculateGdpPerCapita("BRA", 2022));

        // Assert
        assertThat(thrown).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("BRA")
                .hasMessageContaining("2022");
    }

    @Test
    void test_calculateGdpPerCapita_throwsDataNotFoundException_whenThereIsNoPopulationData() {
        // Arrange
        when(observationsClient.getObservations("BRA", GDP_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observation(GDP_INDICATOR, 2022, 1_920_000_000_000.0)));
        when(observationsClient.getObservations("BRA", POPULATION_INDICATOR, 2022, 2022))
                .thenReturn(List.of());

        // Act
        Throwable thrown = catchThrowable(() -> service.calculateGdpPerCapita("BRA", 2022));

        // Assert
        assertThat(thrown).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void test_calculateGdpPerCapita_throwsDataNotFoundException_whenPopulationIsZero() {
        // Arrange
        when(observationsClient.getObservations("BRA", GDP_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observation(GDP_INDICATOR, 2022, 1_920_000_000_000.0)));
        when(observationsClient.getObservations("BRA", POPULATION_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observation(POPULATION_INDICATOR, 2022, 0.0)));

        // Act
        Throwable thrown = catchThrowable(() -> service.calculateGdpPerCapita("BRA", 2022));

        // Assert
        assertThat(thrown).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void test_calculateGdpPerCapita_propagatesWorldBankApiException_whenCommunicationWithTheApiFails() {
        // Arrange
        WorldBankApiException failure = new WorldBankApiException("service unavailable");
        when(observationsClient.getObservations("BRA", GDP_INDICATOR, 2022, 2022))
                .thenThrow(failure);

        // Act
        Throwable thrown = catchThrowable(() -> service.calculateGdpPerCapita("BRA", 2022));

        // Assert
        assertThat(thrown).isSameAs(failure);
    }

    private static Observation observation(String indicator, int year, double value) {
        return new Observation("BR", "Brazil", "BRA", indicator, indicator, year, value);
    }

    private static Observation observationWithoutValue(String indicator, int year) {
        return new Observation("BR", "Brazil", "BRA", indicator, indicator, year, null);
    }
}
