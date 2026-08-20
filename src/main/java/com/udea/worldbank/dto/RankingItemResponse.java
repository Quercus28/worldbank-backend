package com.udea.worldbank.dto;

/** Una fila del ranking de paises por indicador. */
public record RankingItemResponse(
        int position,
        String countryIso3,
        String countryName,
        double value
) {
}