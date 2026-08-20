package com.udea.worldbank.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Region a la que pertenece un pais en el JSON del Banco Mundial.
 * Los "paises" que en realidad son AGREGADOS (Mundo, regiones, grupos de
 * ingreso) traen id == "NA" y value == "Aggregates". Eso permite distinguir
 * paises reales de agregados en el caso de uso de ranking.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorldBankRegion(String id, @JsonProperty("iso2code") String iso2Code, String value) {
}