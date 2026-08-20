package com.udea.worldbank.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO que refleja EXACTAMENTE la respuesta de {@code GET /latest} de la
 * Frankfurter API (tasas de cambio de referencia del Banco Central Europeo).
 * Ejemplo real:
 * <pre>
 * {"amount":1.0,"base":"USD","date":"2026-08-20","rates":{"EUR":0.85609}}
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FrankfurterLatestRates(
        BigDecimal amount,
        String base,
        String date,
        Map<String, BigDecimal> rates
) {
}
