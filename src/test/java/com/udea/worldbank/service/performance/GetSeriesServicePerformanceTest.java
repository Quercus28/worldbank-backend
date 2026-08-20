package com.udea.worldbank.service.performance;

import com.udea.worldbank.client.ObservationsWorldBankClient;
import com.udea.worldbank.dto.SeriesResponse;
import com.udea.worldbank.model.Observation;
import com.udea.worldbank.service.GetSeriesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pruebas de PERFORMANCE de {@link GetSeriesService}: validan que limpiar
 * (descartar nulos) y reordenar cronologicamente una serie muy larga sigue
 * siendo rapido, aunque en la practica el Banco Mundial nunca devuelva series
 * de este tamaño para un solo pais/indicador.
 */
@ExtendWith(MockitoExtension.class)
@Tag("performance")
class GetSeriesServicePerformanceTest {

    @Mock
    private ObservationsWorldBankClient observationsClient;

    @InjectMocks
    private GetSeriesService service;

    @Test
    void test_getSeries_completesWithinTimeBudget_whenTheSeriesIsLarge() {
        // Arrange: años en orden descendente (como los entrega la API real) y con huecos
        int datasetSize = 50_000;
        long maxDurationMs = 300;

        List<Observation> observations = new ArrayList<>(datasetSize);
        for (int i = 0; i < datasetSize; i++) {
            int year = 1960 + (datasetSize - i);
            Double value = i % 5 == 0 ? null : (double) i; // algunos años sin reporte
            observations.add(new Observation("BR", "Brazil", "BRA", "SP.POP.TOTL", "Population, total", year, value));
        }
        when(observationsClient.getObservations("BRA", "SP.POP.TOTL", null, null)).thenReturn(observations);

        service.getSeries("BRA", "SP.POP.TOTL", null, null); // warm-up

        // Act
        long start = System.nanoTime();
        SeriesResponse result = service.getSeries("BRA", "SP.POP.TOTL", null, null);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert
        assertThat(result.points()).isNotEmpty();
        assertThat(elapsedMs)
                .as("getSeries() con %d observaciones debe resolverse en menos de %d ms, tardo %d ms",
                        datasetSize, maxDurationMs, elapsedMs)
                .isLessThan(maxDurationMs);
    }
}
