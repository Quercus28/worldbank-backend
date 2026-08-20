package com.udea.worldbank.client.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.udea.worldbank.client.WorldBankApiExecutor;
import com.udea.worldbank.exception.WorldBankApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
@Tag("unit")
class WorldBankApiExecutorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec uriSpec;
    @Mock
    private RestClient.RequestHeadersSpec headersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private WorldBankApiExecutor newExecutor() {
        return new WorldBankApiExecutor(restClient);
    }

    @Test
    void test_execute_returnsTheResponseBody_whenTheCallSucceeds() {
        // Arrange
        JsonNode expected = objectMapper.createArrayNode();
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(java.util.function.Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(expected);

        // Act
        JsonNode result = newExecutor().execute(ub -> URI.create("https://api.worldbank.org/v2/country"));

        // Assert
        assertThat(result).isSameAs(expected);
    }

    @Test
    void test_execute_throwsWorldBankApiException_whenTheHttpCallFails() {
        // Arrange
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(java.util.function.Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenThrow(new RestClientException("timeout"));

        // Act
        Throwable thrown = catchThrowable(
                () -> newExecutor().execute(ub -> URI.create("https://api.worldbank.org/v2/country")));

        // Assert
        assertThat(thrown).isInstanceOf(WorldBankApiException.class)
                .hasMessageContaining("timeout")
                .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    void test_extractData_throwsWorldBankApiException_whenRootIsNull() {
        // Act
        Throwable thrown = catchThrowable(() -> newExecutor().extractData(null));

        // Assert
        assertThat(thrown).isInstanceOf(WorldBankApiException.class)
                .hasMessageContaining("Unexpected response format");
    }

    @Test
    void test_extractData_throwsWorldBankApiException_whenRootIsNotAnArray() {
        // Arrange
        JsonNode root = objectMapper.createObjectNode();

        // Act
        Throwable thrown = catchThrowable(() -> newExecutor().extractData(root));

        // Assert
        assertThat(thrown).isInstanceOf(WorldBankApiException.class)
                .hasMessageContaining("Unexpected response format");
    }

    @Test
    void test_extractData_throwsWorldBankApiException_whenRootIsAnEmptyArray() {
        // Arrange
        ArrayNode root = objectMapper.createArrayNode();

        // Act
        Throwable thrown = catchThrowable(() -> newExecutor().extractData(root));

        // Assert
        assertThat(thrown).isInstanceOf(WorldBankApiException.class)
                .hasMessageContaining("Unexpected response format");
    }

    @Test
    void test_extractData_throwsWorldBankApiException_whenMetadataCarriesAnErrorMessage() {
        // Arrange
        ArrayNode root = objectMapper.createArrayNode();
        ObjectNode metadata = root.addObject();
        metadata.put("message", "Invalid value");

        // Act
        Throwable thrown = catchThrowable(() -> newExecutor().extractData(root));

        // Assert
        assertThat(thrown).isInstanceOf(WorldBankApiException.class)
                .hasMessageContaining("The API returned an error");
    }

    @Test
    void test_extractData_returnsNull_whenThereIsNoDataPosition() {
        // Arrange
        ArrayNode root = objectMapper.createArrayNode();
        root.addObject();

        // Act
        JsonNode result = newExecutor().extractData(root);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void test_extractData_returnsNull_whenTheDataPositionIsNull() {
        // Arrange
        ArrayNode root = objectMapper.createArrayNode();
        root.addObject();
        root.addNull();

        // Act
        JsonNode result = newExecutor().extractData(root);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void test_extractData_returnsPositionOne_whenTheResponseIsValid() {
        // Arrange
        ArrayNode root = objectMapper.createArrayNode();
        root.addObject();
        ArrayNode data = root.addArray();
        data.addObject().put("id", "BRA");

        // Act
        JsonNode result = newExecutor().extractData(root);

        // Assert
        assertThat(result).isSameAs(data);
    }
}
