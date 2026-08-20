package com.udea.worldbank.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO de un elemento del endpoint /country. Solo se usan los campos necesarios
 * para el caso de uso de ranking (código ISO3 y region para descartar agregados).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorldBankCountry(
        String id,
        @JsonProperty("iso2Code") String iso2Code,
        String name,
        WorldBankRegion region
) {

    /** Un agregado (Mundo, region, grupo de ingreso) trae region id "NA". */
    public boolean isAggregate() {
        return region == null || region.id() == null || "NA".equals(region.id());
    }
}