package com.udea.worldbank.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO que refleja EXACTAMENTE un elemento del array de datos que devuelve la
 * World Bank Indicators API. Ejemplo real de un elemento:
 * <pre>
 * {
 *   "indicator": {"id": "SP.POP.TOTL", "value": "Population, total"},
 *   "country":   {"id": "BR", "value": "Brazil"},
 *   "countryiso3code": "BRA",
 *   "date": "2020",
 *   "value": 212559409,
 *   "unit": "",
 *   "obs_status": "",
 *   "decimal": 0
 * }
 * </pre>
 * "value" llega como null cuando el pais no reporto dato ese año.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorldBankObservation(
        WorldBankRef indicator,
        WorldBankRef country,
        @JsonProperty("countryiso3code") String countryIso3Code,
        String date,
        Double value,
        String unit,
        @JsonProperty("obs_status") String obsStatus,
        Integer decimal
) {
}