package com.udea.worldbank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configura el cliente HTTP (RestClient, sincrono) que usa la capa de acceso a
 * datos. Se inyecta la base URL desde application.properties.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient worldBankRestClient(
            RestClient.Builder builder,
            @Value("${worldbank.api.base-url}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
