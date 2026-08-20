package com.udea.worldbank.client.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.udea.worldbank.client.ObservationsWorldBankClient;
import com.udea.worldbank.client.WorldBankApiExecutor;
import com.udea.worldbank.client.mapper.ObservationMapper;
import com.udea.worldbank.model.Observation;
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
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ObservationsWorldBankClientTest {

    @Mock
    private WorldBankApiExecutor executor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObservationMapper mapper = new ObservationMapper();

    private ObservationsWorldBankClient client;

    @BeforeEach
    void setUp() {
        client = new ObservationsWorldBankClient(executor, objectMapper, mapper);
    }

    @Test
    void test_getObservations_returnsMappedObservations_whenTheApiReturnsData() {
        // Arrange
        ArrayNode root = objectMapper.createArrayNode();
        root.addObject();
        ArrayNode data = root.addArray();
        ObjectNode item = data.addObject();
        item.putObject("indicator").put("id", "SP.POP.TOTL").put("value", "Population, total");
        item.putObject("country").put("id", "BR").put("value", "Brazil");
        item.put("countryiso3code", "BRA");
        item.put("date", "2020");
        item.put("value", 212559409.0);

        when(executor.execute(any())).thenReturn(root);
        when(executor.extractData(root)).thenReturn(data);

        // Act
        List<Observation> result = client.getObservations("BRA", "SP.POP.TOTL", 2020, 2020);

        // Assert
        assertThat(result).hasSize(1);
        Observation obs = result.get(0);
        assertThat(obs.countryIso3()).isEqualTo("BRA");
        assertThat(obs.year()).isEqualTo(2020);
        assertThat(obs.value()).isEqualTo(212559409.0);
    }

    @Test
    void test_getObservations_returnsEmptyList_whenTheApiReturnsNoData() {
        // Arrange
        ArrayNode root = objectMapper.createArrayNode();
        when(executor.execute(any())).thenReturn(root);
        when(executor.extractData(root)).thenReturn(null);

        // Act
        List<Observation> result = client.getObservations("BRA", "SP.POP.TOTL", null, null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_getObservations_omitsTheDateParameter_whenFromAndToAreNull() {
        // Arrange
        ArgumentCaptor<Function<UriBuilder, URI>> captor = ArgumentCaptor.forClass(Function.class);
        when(executor.execute(captor.capture())).thenReturn(objectMapper.createArrayNode());
        when(executor.extractData(any())).thenReturn(null);

        // Act
        client.getObservations("BRA", "SP.POP.TOTL", null, null);
        URI uri = captor.getValue().apply(uriBuilder());

        // Assert
        assertThat(uri.toString())
                .contains("/country/BRA/indicator/SP.POP.TOTL")
                .doesNotContain("date=");
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_getObservations_omitsTheDateParameter_whenOnlyOneOfFromOrToIsNull() {
        // Arrange
        ArgumentCaptor<Function<UriBuilder, URI>> captor = ArgumentCaptor.forClass(Function.class);
        when(executor.execute(captor.capture())).thenReturn(objectMapper.createArrayNode());
        when(executor.extractData(any())).thenReturn(null);

        // Act
        client.getObservations("BRA", "SP.POP.TOTL", 2010, null);
        URI uri = captor.getValue().apply(uriBuilder());

        // Assert
        assertThat(uri.toString()).doesNotContain("date=");
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_getObservations_includesTheDateRange_whenFromAndToAreNotNull() {
        // Arrange
        ArgumentCaptor<Function<UriBuilder, URI>> captor = ArgumentCaptor.forClass(Function.class);
        when(executor.execute(captor.capture())).thenReturn(objectMapper.createArrayNode());
        when(executor.extractData(any())).thenReturn(null);

        // Act
        client.getObservations("BRA", "SP.POP.TOTL", 2010, 2020);
        URI uri = captor.getValue().apply(uriBuilder());

        // Assert
        assertThat(uri.toString()).contains("date=2010:2020");
    }

    private static UriBuilder uriBuilder() {
        return new DefaultUriBuilderFactory("https://api.worldbank.org/v2").builder();
    }
}
