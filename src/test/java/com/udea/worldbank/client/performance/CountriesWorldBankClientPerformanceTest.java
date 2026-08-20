package com.udea.worldbank.client.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.udea.worldbank.client.CountriesWorldBankClient;
import com.udea.worldbank.client.WorldBankApiExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de PERFORMANCE del cache de {@link CountriesWorldBankClient}:
 * una vez resuelto el cache, la lista de paises reales debe soportar alta
 * concurrencia de lectura sin volver a golpear el {@link WorldBankApiExecutor}
 * y sin degradar el throughput bajo contencion.
 */
@ExtendWith(MockitoExtension.class)
@Tag("performance")
class CountriesWorldBankClientPerformanceTest {

    @Mock
    private WorldBankApiExecutor executor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CountriesWorldBankClient client;

    @BeforeEach
    void setUp() {
        client = new CountriesWorldBankClient(executor, objectMapper);
    }

    @Test
    void test_getRealCountryCodes_sustainsHighConcurrentThroughput_onceTheCacheIsWarm() throws Exception {
        // Arrange
        ArrayNode root = objectMapper.createArrayNode();
        ArrayNode data = objectMapper.createArrayNode();
        ObjectNode brazil = data.addObject();
        brazil.put("id", "BRA").put("iso2Code", "BR").put("name", "Brazil");
        brazil.putObject("region").put("id", "LCN").put("iso2code", "ZJ").put("value", "Latin America");
        when(executor.execute(any())).thenReturn(root);
        when(executor.extractData(root)).thenReturn(data);

        client.getRealCountryCodes(); // calienta el cache

        int threads = 32;
        int callsPerThread = 2_000;
        long maxDurationMs = 3_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Set<String>>> tasks = IntStream.range(0, threads)
                    .<Callable<Set<String>>>mapToObj(t -> () -> {
                        Set<String> last = null;
                        for (int i = 0; i < callsPerThread; i++) {
                            last = client.getRealCountryCodes();
                        }
                        return last;
                    })
                    .collect(Collectors.toList());

            // Act
            long start = System.nanoTime();
            List<Future<Set<String>>> results = pool.invokeAll(tasks);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            // Assert
            for (Future<Set<String>> f : results) {
                assertThat(f.get(5, TimeUnit.SECONDS)).containsExactly("BRA");
            }
            int totalCalls = threads * callsPerThread;
            assertThat(elapsedMs)
                    .as("%d lecturas concurrentes del cache deben completarse en menos de %d ms, tardaron %d ms",
                            totalCalls, maxDurationMs, elapsedMs)
                    .isLessThan(maxDurationMs);
            verify(executor, times(1)).execute(any()); // el cache evita volver a golpear el executor
        } finally {
            pool.shutdownNow();
        }
    }
}
