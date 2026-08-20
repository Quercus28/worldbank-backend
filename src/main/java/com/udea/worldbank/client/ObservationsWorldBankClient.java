package com.udea.worldbank.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.worldbank.client.dto.WorldBankObservation;
import com.udea.worldbank.client.mapper.ObservationMapper;
import com.udea.worldbank.model.Observation;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CASO DE USO: obtener las observaciones de un indicador para uno o varios
 * paises. Traduce el JSON crudo a objetos de dominio via {@link ObservationMapper}.
 */
@Component
public class ObservationsWorldBankClient {

    private final WorldBankApiExecutor executor;
    private final ObjectMapper objectMapper;
    private final ObservationMapper mapper;

    public ObservationsWorldBankClient(WorldBankApiExecutor executor,
                                       ObjectMapper objectMapper,
                                       ObservationMapper mapper) {
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
    }

    /**
     * Obtiene las observaciones de un indicador para uno o varios paises.
     *
     * @param countries codigo(s) de pais separados por ';' o el literal "all"
     * @param indicator codigo del indicador (p.ej. "SP.POP.TOTL")
     * @param from      año inicial (inclusive) o null para no acotar
     * @param to        año final (inclusive) o null para no acotar
     */
    public List<Observation> getObservations(String countries, String indicator,
                                             Integer from, Integer to) {
        JsonNode root = executor.execute(uriBuilder -> {
            uriBuilder.path("/country/{countries}/indicator/{indicator}")
                    .queryParam("format", "json")
                    .queryParam("per_page", WorldBankApiExecutor.MAX_PAGE_SIZE);
            if (from != null && to != null) {
                uriBuilder.queryParam("date", from + ":" + to);
            }
            return uriBuilder.build(countries, indicator);
        });

        JsonNode data = executor.extractData(root);
        if (data == null) {
            return List.of();
        }
        List<WorldBankObservation> raw =
                objectMapper.convertValue(data, new TypeReference<List<WorldBankObservation>>() {});
        return raw.stream().map(mapper::toDomain).toList();
    }
}