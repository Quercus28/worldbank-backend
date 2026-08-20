package com.udea.worldbank.config.unit;

import com.udea.worldbank.config.RestClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class RestClientConfigTest {

    private final RestClientConfig config = new RestClientConfig();

    @Test
    void test_worldBankRestClient_buildsANonNullClient_withTheConfiguredBaseUrl() {
        // Arrange
        RestClient.Builder builder = RestClient.builder();

        // Act
        RestClient client = config.worldBankRestClient(builder, "https://api.worldbank.org/v2");

        // Assert
        assertThat(client).isNotNull();
    }
}
