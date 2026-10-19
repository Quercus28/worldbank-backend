package com.udea.worldbank.controller.performance;

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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pruebas de PERFORMANCE del controlador: con los casos de uso mockeados
 * (respuesta inmediata), validan que la capa web no introduce overhead
 * relevante y que sigue siendo correcta bajo carga concurrente (muchos hilos
 * golpeando los cuatro endpoints a la vez).
 */
@ExtendWith(MockitoExtension.class)
@Tag("performance")
class IndicatorControllerPerformanceTest {

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
    void test_endpoints_sustainConcurrentThroughput_whenManyRequestsArriveAtOnce() throws Exception {
        // Arrange
        SeriesResponse series = new SeriesResponse("BRA", "Brazil", "SP.POP.TOTL", "Population, total", List.of());
        GdpPerCapitaResponse gdpPerCapita = new GdpPerCapitaResponse(
                "BRA", 2022, BigDecimal.ONE, "$1.00", 1L, BigDecimal.ONE, "$1.00",
                null, null, null, null, null);
        List<RankingItemResponse> ranking = List.of(new RankingItemResponse(1, "USA", "United States", 1.0));
        ExchangeRateResponse exchangeRate = new ExchangeRateResponse(
                "USD", "EUR", new BigDecimal("0.85609"), BigDecimal.ONE, new BigDecimal("0.86"), "€0.86", "2026-08-20");

        when(getSeriesUseCase.getSeries("BRA", "SP.POP.TOTL", 2010, 2020)).thenReturn(series);
        when(calculateGdpPerCapitaUseCase.calculateGdpPerCapita("BRA", 2022, null)).thenReturn(gdpPerCapita);
        when(rankingUseCase.ranking("NY.GDP.MKTP.CD", 2022, 10)).thenReturn(ranking);
        when(exchangeRateUseCase.convert("USD", "EUR", BigDecimal.ONE)).thenReturn(exchangeRate);

        int threads = 50;
        int requestsPerThread = 200;
        long maxDurationMs = 5_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Boolean>> tasks = IntStream.range(0, threads)
                    .<Callable<Boolean>>mapToObj(t -> (Callable<Boolean>) () -> {
                        for (int i = 0; i < requestsPerThread; i++) {
                            boolean ok = switch (i % 4) {
                                case 0 -> controller.series("BRA", "SP.POP.TOTL", 2010, 2020) == series;
                                case 1 -> controller.gdpPerCapita("BRA", 2022, null) == gdpPerCapita;
                                case 2 -> controller.ranking("NY.GDP.MKTP.CD", 2022, 10) == ranking;
                                default -> controller.exchangeRate("USD", "EUR", BigDecimal.ONE) == exchangeRate;
                            };
                            if (!ok) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .toList();

            // Act
            long start = System.nanoTime();
            List<Future<Boolean>> results = pool.invokeAll(tasks);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            // Assert
            for (Future<Boolean> f : results) {
                assertThat(f.get(5, TimeUnit.SECONDS)).isTrue();
            }
            int totalRequests = threads * requestsPerThread;
            assertThat(elapsedMs)
                    .as("%d peticiones concurrentes deben completarse en menos de %d ms, tardaron %d ms",
                            totalRequests, maxDurationMs, elapsedMs)
                    .isLessThan(maxDurationMs);
        } finally {
            pool.shutdownNow();
        }
    }
}
