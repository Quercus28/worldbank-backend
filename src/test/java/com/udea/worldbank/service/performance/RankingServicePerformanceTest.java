package com.udea.worldbank.service.performance;

import com.udea.worldbank.client.CountriesWorldBankClient;
import com.udea.worldbank.client.ObservationsWorldBankClient;
import com.udea.worldbank.dto.RankingItemResponse;
import com.udea.worldbank.model.Observation;
import com.udea.worldbank.service.RankingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pruebas de PERFORMANCE de {@link RankingService}: validan que el
 * procesamiento (filtrado de nulos, cruce con la lista de paises reales,
 * orden descendente y recorte al limite) siga siendo rapido incluso con un
 * dataset mucho mas grande que el universo real de paises/agregados del
 * Banco Mundial (~300 entradas), y que se sostenga bajo muchas llamadas
 * seguidas.
 */
@ExtendWith(MockitoExtension.class)
@Tag("performance")
class RankingServicePerformanceTest {

    private static final String INDICATOR = "NY.GDP.MKTP.CD";

    @Mock
    private ObservationsWorldBankClient observationsClient;

    @Mock
    private CountriesWorldBankClient countriesClient;

    @InjectMocks
    private RankingService service;

    @Test
    void test_ranking_completesWithinTimeBudget_whenTheDatasetIsLarge() {
        // Arrange
        int datasetSize = 20_000;
        long maxDurationMs = 300;

        List<Observation> observations = new ArrayList<>(datasetSize);
        Set<String> realCountryCodes = new HashSet<>();
        for (int i = 0; i < datasetSize; i++) {
            String iso3 = "C%05d".formatted(i);
            boolean isRealCountry = i % 3 != 0; // 1 de cada 3 simula un agregado regional
            if (isRealCountry) {
                realCountryCodes.add(iso3);
            }
            Double value = i % 7 == 0 ? null : (double) (datasetSize - i); // algunos sin dato reportado
            observations.add(new Observation(iso3, "Country " + i, iso3, INDICATOR, "GDP", 2022, value));
        }
        when(observationsClient.getObservations("all", INDICATOR, 2022, 2022)).thenReturn(observations);
        when(countriesClient.getRealCountryCodes()).thenReturn(realCountryCodes);

        service.ranking(INDICATOR, 2022, 10); // warm-up: evita medir la carga de clases / JIT en frio

        // Act
        long start = System.nanoTime();
        List<RankingItemResponse> result = service.ranking(INDICATOR, 2022, 10);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert
        assertThat(result).hasSize(10);
        assertThat(elapsedMs)
                .as("ranking() con %d observaciones debe resolverse en menos de %d ms, tardo %d ms",
                        datasetSize, maxDurationMs, elapsedMs)
                .isLessThan(maxDurationMs);
    }

    @Test
    void test_ranking_sustainsThroughput_underManySequentialCalls() {
        // Arrange: dataset moderado, muchas invocaciones seguidas simulando carga sostenida
        int size = 500;
        int calls = 1_000;
        long maxTotalMs = 2_000;

        List<Observation> observations = new ArrayList<>(size);
        Set<String> realCountryCodes = new HashSet<>();
        for (int i = 0; i < size; i++) {
            String iso3 = "C%05d".formatted(i);
            realCountryCodes.add(iso3);
            observations.add(new Observation(iso3, "Country " + i, iso3, INDICATOR, "GDP", 2022, (double) i));
        }
        when(observationsClient.getObservations("all", INDICATOR, 2022, 2022)).thenReturn(observations);
        when(countriesClient.getRealCountryCodes()).thenReturn(realCountryCodes);

        // Act
        long start = System.nanoTime();
        for (int i = 0; i < calls; i++) {
            service.ranking(INDICATOR, 2022, 10);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert
        assertThat(elapsedMs)
                .as("%d llamadas secuenciales a ranking() deben completarse en menos de %d ms, tardaron %d ms",
                        calls, maxTotalMs, elapsedMs)
                .isLessThan(maxTotalMs);
    }
}
