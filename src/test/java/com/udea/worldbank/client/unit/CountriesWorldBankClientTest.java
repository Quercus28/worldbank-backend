package com.udea.worldbank.client.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.udea.worldbank.client.CountriesWorldBankClient;
import com.udea.worldbank.client.WorldBankApiExecutor;
import com.udea.worldbank.exception.WorldBankApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CountriesWorldBankClientTest {

    @Mock
    private WorldBankApiExecutor executor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CountriesWorldBankClient client;

    @BeforeEach
    void setUp() {
        client = new CountriesWorldBankClient(executor, objectMapper);
    }

    @Test
    void test_getRealCountryCodes_excludesAggregates_whenTheApiReturnsCountriesAndAggregates() {
        // Arrange
        ArrayNode root = objectMapper.createArrayNode();
        ArrayNode data = objectMapper.createArrayNode();

        ObjectNode brazil = data.addObject();
        brazil.put("id", "BRA").put("iso2Code", "BR").put("name", "Brazil");
        brazil.putObject("region").put("id", "LCN").put("iso2code", "ZJ").put("value", "Latin America");

        ObjectNode world = data.addObject();
        world.put("id", "WLD").put("iso2Code", "1W").put("name", "World");
        world.putObject("region").put("id", "NA").put("iso2code", "NA").put("value", "Aggregates");

        when(executor.execute(any())).thenReturn(root);
        when(executor.extractData(root)).thenReturn(data);

        // Act
        Set<String> result = client.getRealCountryCodes();

        // Assert
        assertThat(result).containsExactly("BRA");
    }

    @Test
    void test_getRealCountryCodes_cachesTheResult_whenCalledMoreThanOnce() {
        // Arrange
        ArrayNode root = objectMapper.createArrayNode();
        ArrayNode data = objectMapper.createArrayNode();
        ObjectNode brazil = data.addObject();
        brazil.put("id", "BRA").put("iso2Code", "BR").put("name", "Brazil");
        brazil.putObject("region").put("id", "LCN").put("iso2code", "ZJ").put("value", "Latin America");

        when(executor.execute(any())).thenReturn(root);
        when(executor.extractData(root)).thenReturn(data);

        // Act
        Set<String> first = client.getRealCountryCodes();
        Set<String> second = client.getRealCountryCodes();

        // Assert
        assertThat(second).isSameAs(first);
        verify(executor, times(1)).execute(any());
    }

    @Test
    void test_getRealCountryCodes_throwsWorldBankApiException_whenTheApiReturnsNoData() {
        // Arrange
        when(executor.execute(any())).thenReturn(objectMapper.createArrayNode());
        when(executor.extractData(any())).thenReturn(null);

        // Act
        Throwable thrown = catchThrowable(() -> client.getRealCountryCodes());

        // Assert
        assertThat(thrown).isInstanceOf(WorldBankApiException.class)
                .hasMessageContaining("Could not fetch the list of countries");
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_getRealCountryCodes_queriesTheCountryEndpointWithTheExpectedParameters() {
        // Arrange
        ArgumentCaptor<Function<UriBuilder, URI>> captor = ArgumentCaptor.forClass(Function.class);
        ArrayNode root = objectMapper.createArrayNode();
        ArrayNode data = objectMapper.createArrayNode();
        when(executor.execute(captor.capture())).thenReturn(root);
        when(executor.extractData(root)).thenReturn(data);

        // Act
        client.getRealCountryCodes();
        URI uri = captor.getValue().apply(new DefaultUriBuilderFactory("https://api.worldbank.org/v2").builder());

        // Assert
        assertThat(uri.toString())
                .contains("/country")
                .contains("format=json")
                .contains("per_page=20000");
    }

    @Test
    void test_getRealCountryCodes_reusesTheFirstThreadResult_whenTwoThreadsRaceInTheSynchronizedSection()
            throws Exception {
        // Arrange: el primer hilo se queda "dentro" de la seccion sincronizada
        // (bloqueado en la llamada a la API) hasta que el test confirma que el
        // segundo hilo ya quedo bloqueado esperando el lock, para forzar a que
        // ejercite la doble verificacion del cache.
        ArrayNode root = objectMapper.createArrayNode();
        ArrayNode data = objectMapper.createArrayNode();
        ObjectNode brazil = data.addObject();
        brazil.put("id", "BRA").put("iso2Code", "BR").put("name", "Brazil");
        brazil.putObject("region").put("id", "LCN").put("iso2code", "ZJ").put("value", "Latin America");

        CountDownLatch firstThreadInside = new CountDownLatch(1);
        CountDownLatch releaseFirstThread = new CountDownLatch(1);
        when(executor.execute(any())).thenAnswer(invocation -> {
            firstThreadInside.countDown();
            releaseFirstThread.await(2, TimeUnit.SECONDS);
            return root;
        });
        when(executor.extractData(root)).thenReturn(data);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // Act
            Future<Set<String>> first = pool.submit(client::getRealCountryCodes);
            assertThat(firstThreadInside.await(2, TimeUnit.SECONDS)).isTrue();

            AtomicReference<Thread> secondThread = new AtomicReference<>();
            Future<Set<String>> second = pool.submit(() -> {
                secondThread.set(Thread.currentThread());
                return client.getRealCountryCodes();
            });
            await().atMost(Duration.ofSeconds(2))
                    .until(() -> secondThread.get() != null && secondThread.get().getState() == Thread.State.BLOCKED);
            releaseFirstThread.countDown();

            Set<String> result1 = first.get(3, TimeUnit.SECONDS);
            Set<String> result2 = second.get(3, TimeUnit.SECONDS);

            // Assert
            assertThat(result2).isSameAs(result1);
            verify(executor, times(1)).execute(any());
        } finally {
            pool.shutdownNow();
        }
    }
}
