package com.udea.worldbank.service.performance;

import com.udea.worldbank.client.ExchangeRateClient;
import com.udea.worldbank.client.ObservationsWorldBankClient;
import com.udea.worldbank.client.dto.FrankfurterLatestRates;
import com.udea.worldbank.dto.GdpPerCapitaResponse;
import com.udea.worldbank.model.Observation;
import com.udea.worldbank.service.CalculateGdpPerCapitaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pruebas de PERFORMANCE de {@link CalculateGdpPerCapitaService}: el calculo
 * es O(1) (dos observaciones puntuales, una division y, si se pide, una
 * conversion de moneda), asi que lo que se valida es que sostiene un alto
 * volumen de llamadas secuenciales y concurrentes sin degradarse -- tanto con
 * como sin conversion de moneda.
 */
@ExtendWith(MockitoExtension.class)
@Tag("performance")
class CalculateGdpPerCapitaServicePerformanceTest {

    private static final String GDP_INDICATOR = "NY.GDP.MKTP.CD";
    private static final String POPULATION_INDICATOR = "SP.POP.TOTL";

    @Mock
    private ObservationsWorldBankClient observationsClient;
    @Mock
    private ExchangeRateClient exchangeRateClient;

    @InjectMocks
    private CalculateGdpPerCapitaService service;

    @Test
    void test_calculateGdpPerCapita_sustainsThroughput_underManySequentialCallsWithoutConversion() {
        // Arrange
        int calls = 5_000;
        long maxTotalMs = 1_000;
        stubGdpAndPopulation();

        service.calculateGdpPerCapita("BRA", 2022, null); // warm-up

        // Act
        long start = System.nanoTime();
        for (int i = 0; i < calls; i++) {
            service.calculateGdpPerCapita("BRA", 2022, null);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert
        assertThat(elapsedMs)
                .as("%d llamadas secuenciales sin conversion deben completarse en menos de %d ms, tardaron %d ms",
                        calls, maxTotalMs, elapsedMs)
                .isLessThan(maxTotalMs);
    }

    @Test
    void test_calculateGdpPerCapita_sustainsThroughput_underManySequentialCallsWithConversion() {
        // Arrange
        int calls = 5_000;
        long maxTotalMs = 1_500;
        stubGdpAndPopulation();
        stubExchangeRate();

        service.calculateGdpPerCapita("BRA", 2022, "EUR"); // warm-up

        // Act
        long start = System.nanoTime();
        GdpPerCapitaResponse result = null;
        for (int i = 0; i < calls; i++) {
            result = service.calculateGdpPerCapita("BRA", 2022, "EUR");
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert
        assertThat(result.targetCurrency()).isEqualTo("EUR");
        assertThat(elapsedMs)
                .as("%d llamadas secuenciales con conversion deben completarse en menos de %d ms, tardaron %d ms",
                        calls, maxTotalMs, elapsedMs)
                .isLessThan(maxTotalMs);
    }

    @Test
    void test_calculateGdpPerCapita_sustainsHighConcurrentThroughput_whenManyThreadsCalculateAtOnce() throws Exception {
        // Arrange: la mitad de las llamadas de cada hilo piden conversion y la otra mitad no
        stubGdpAndPopulation();
        stubExchangeRate();

        int threads = 32;
        int callsPerThread = 500;
        long maxDurationMs = 3_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Boolean>> tasks = IntStream.range(0, threads)
                    .<Callable<Boolean>>mapToObj(t -> () -> {
                        for (int i = 0; i < callsPerThread; i++) {
                            String currency = i % 2 == 0 ? null : "EUR";
                            GdpPerCapitaResponse result = service.calculateGdpPerCapita("BRA", 2022, currency);
                            boolean ok = result.gdpPerCapitaUsd().compareTo(new BigDecimal("8930.23")) == 0
                                    && (currency == null) == (result.targetCurrency() == null);
                            if (!ok) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            // Act
            long start = System.nanoTime();
            List<Future<Boolean>> results = pool.invokeAll(tasks);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            // Assert
            for (Future<Boolean> f : results) {
                assertThat(f.get(5, TimeUnit.SECONDS)).isTrue();
            }
            int totalCalls = threads * callsPerThread;
            assertThat(elapsedMs)
                    .as("%d calculos concurrentes deben completarse en menos de %d ms, tardaron %d ms",
                            totalCalls, maxDurationMs, elapsedMs)
                    .isLessThan(maxDurationMs);
        } finally {
            pool.shutdownNow();
        }
    }

    private void stubGdpAndPopulation() {
        when(observationsClient.getObservations("BRA", GDP_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observation(GDP_INDICATOR, 1_920_000_000_000.0)));
        when(observationsClient.getObservations("BRA", POPULATION_INDICATOR, 2022, 2022))
                .thenReturn(List.of(observation(POPULATION_INDICATOR, 215_000_000.0)));
    }

    private void stubExchangeRate() {
        when(exchangeRateClient.getLatestRate("USD", "EUR")).thenReturn(new FrankfurterLatestRates(
                BigDecimal.ONE, "USD", "2026-08-20", Map.of("EUR", new BigDecimal("0.85609"))));
    }

    private static Observation observation(String indicator, double value) {
        return new Observation("BR", "Brazil", "BRA", indicator, indicator, 2022, value);
    }
}
