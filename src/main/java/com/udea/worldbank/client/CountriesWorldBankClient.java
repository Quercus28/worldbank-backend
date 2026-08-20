package com.udea.worldbank.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.worldbank.client.dto.WorldBankCountry;
import com.udea.worldbank.exception.WorldBankApiException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CASO DE USO: obtener los codigos ISO3 de los paises reales (sin agregados
 * regionales ni grupos de ingreso). La lista es estable, asi que se cachea
 * tras la primera llamada.
 */
@Component
public class CountriesWorldBankClient {

    private final WorldBankApiExecutor executor;
    private final ObjectMapper objectMapper;

    private volatile Set<String> cachedRealCountryCodes;

    public CountriesWorldBankClient(WorldBankApiExecutor executor, ObjectMapper objectMapper) {
        this.executor = executor;
        this.objectMapper = objectMapper;
    }

    public Set<String> getRealCountryCodes() {
        Set<String> cache = cachedRealCountryCodes;
        if (cache != null) {
            return cache;
        }
        synchronized (this) {
            if (cachedRealCountryCodes != null) {
                return cachedRealCountryCodes;
            }
            JsonNode root = executor.execute(uriBuilder -> uriBuilder
                    .path("/country")
                    .queryParam("format", "json")
                    .queryParam("per_page", WorldBankApiExecutor.MAX_PAGE_SIZE)
                    .build());

            JsonNode data = executor.extractData(root);
            if (data == null) {
                throw new WorldBankApiException("Could not fetch the list of countries");
            }
            List<WorldBankCountry> countries =
                    objectMapper.convertValue(data, new TypeReference<List<WorldBankCountry>>() {});
            cachedRealCountryCodes = countries.stream()
                    .filter(c -> !c.isAggregate())
                    .map(WorldBankCountry::id)
                    .collect(Collectors.toUnmodifiableSet());
            return cachedRealCountryCodes;
        }
    }
}