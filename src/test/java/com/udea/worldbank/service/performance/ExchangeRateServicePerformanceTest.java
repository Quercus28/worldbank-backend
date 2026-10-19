package com.udea.worldbank.service.performance;

import com.udea.worldbank.client.ExchangeRateClient;
import com.udea.worldbank.client.dto.FrankfurterLatestRates;
import com.udea.worldbank.dto.ExchangeRateResponse;
import com.udea.worldbank.service.ExchangeRateService;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de PERFORMANCE de {@link ExchangeRateService}: el calculo en si es
 * O(1) (una consulta a un mapa y una multiplicacion), asi que lo que se
 * valida es que sostiene un alto volumen de llamadas secuenciales y
 * concurrentes sin degradarse, y que el atajo de "misma moneda" realmente
 * evita golpear la API externa bajo carga concurrente (no solo en una
 * llamada aislada).
 */
@ExtendWith(MockitoExtension.class)
@Tag("performance")
class ExchangeRateServicePerformanceTest {

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @InjectMocks
    private ExchangeRateService service;

    @Test
    void test_convert_sustainsThroughput_underManySequentialCalls() {
        // Arrange
        int calls = 5_000;
        long maxTotalMs = 1_000;
        when(exchangeRateClient.getLatestRate("USD", "EUR")).thenReturn(new FrankfurterLatestRates(
                BigDecimal.ONE, "USD", "2026-08-20", Map.of("EUR", new BigDecimal("0.85609"))));

        service.convert("USD", "EUR", BigDecimal.TEN); // warm-up: evita medir la carga de clases / JIT en frio

        // Act
        long start = System.nanoTime();
        for (int i = 0; i < calls; i++) {
            service.convert("USD", "EUR", BigDecimal.TEN);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert
        assertThat(elapsedMs)
                .as("%d llamadas secuenciales a convert() deben completarse en menos de %d ms, tardaron %d ms",
                        calls, maxTotalMs, elapsedMs)
                .isLessThan(maxTotalMs);
    }

    @Test
    void test_convert_sustainsHighConcurrentThroughput_whenManyThreadsConvertAtOnce() throws Exception {
        // Arrange
        when(exchangeRateClient.getLatestRate("USD", "EUR")).thenReturn(new FrankfurterLatestRates(
                BigDecimal.ONE, "USD", "2026-08-20", Map.of("EUR", new BigDecimal("0.85609"))));

        int threads = 32;
        int callsPerThread = 1_000;
        long maxDurationMs = 3_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<ExchangeRateResponse>> tasks = IntStream.range(0, threads)
                    .<Callable<ExchangeRateResponse>>mapToObj(t -> () -> {
                        ExchangeRateResponse last = null;
                        for (int i = 0; i < callsPerThread; i++) {
                            last = service.convert("USD", "EUR", BigDecimal.TEN);
                        }
                        return last;
                    })
                    .toList();

            // Act
            long start = System.nanoTime();
            List<Future<ExchangeRateResponse>> results = pool.invokeAll(tasks);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            // Assert
            for (Future<ExchangeRateResponse> f : results) {
                assertThat(f.get(5, TimeUnit.SECONDS).convertedAmount()).isEqualByComparingTo("8.56");
            }
            int totalCalls = threads * callsPerThread;
            assertThat(elapsedMs)
                    .as("%d conversiones concurrentes deben completarse en menos de %d ms, tardaron %d ms",
                            totalCalls, maxDurationMs, elapsedMs)
                    .isLessThan(maxDurationMs);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void test_convert_neverCallsTheExternalApi_underConcurrentLoad_whenBothCurrenciesAreTheSame() throws Exception {
        // Arrange: el atajo de "misma moneda" (ExchangeRateService.convert) debe
        // sostenerse bajo concurrencia, no solo en una llamada aislada.
        int threads = 32;
        int callsPerThread = 1_000;
        long maxDurationMs = 2_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Boolean>> tasks = IntStream.range(0, threads)
                    .<Callable<Boolean>>mapToObj(t -> () -> {
                        for (int i = 0; i < callsPerThread; i++) {
                            ExchangeRateResponse result = service.convert("usd", "USD", BigDecimal.TEN);
                            if (result.convertedAmount().compareTo(new BigDecimal("10.00")) != 0) {
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
            int totalCalls = threads * callsPerThread;
            assertThat(elapsedMs)
                    .as("%d conversiones concurrentes de la misma moneda deben completarse en menos de %d ms, tardaron %d ms",
                            totalCalls, maxDurationMs, elapsedMs)
                    .isLessThan(maxDurationMs);
            verify(exchangeRateClient, never()).getLatestRate(anyString(), anyString());
        } finally {
            pool.shutdownNow();
        }
    }
}
