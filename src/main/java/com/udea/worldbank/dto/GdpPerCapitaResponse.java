package com.udea.worldbank.dto;

import java.math.BigDecimal;

/**
 * Resultado del indicador DERIVADO: PIB per capita = PIB / poblacion.
 * Es un dato que la API NO expone directamente; lo calcula la capa de servicio.
 *
 * @param gdpUsd                          PIB en dolares, como numero (para que el cliente pueda calcular con el)
 * @param gdpUsdFormatted                 el mismo PIB, formateado como moneda para mostrar (p.ej. "$1,951,923,942,083.32")
 * @param gdpPerCapitaUsd                 PIB per capita en dolares, como numero
 * @param gdpPerCapitaUsdFormatted        el mismo PIB per capita, formateado como moneda para mostrar
 * @param targetCurrency                  moneda pedida para la conversion (codigo ISO 4217), o {@code null} si no se pidio
 * @param gdpPerCapitaConverted           PIB per capita convertido a {@code targetCurrency}, o {@code null} si no se pidio
 * @param gdpPerCapitaConvertedFormatted  el mismo monto convertido, formateado como moneda, o {@code null} si no se pidio
 * @param exchangeRate                    tasa de cambio USD -> {@code targetCurrency} usada, o {@code null} si no se pidio
 * @param exchangeRateDate                fecha de publicacion de esa tasa, o {@code null} si no se pidio
 */
public record GdpPerCapitaResponse(
        String countryIso3,
        int year,
        BigDecimal gdpUsd,
        String gdpUsdFormatted,
        long population,
        BigDecimal gdpPerCapitaUsd,
        String gdpPerCapitaUsdFormatted,
        String targetCurrency,
        BigDecimal gdpPerCapitaConverted,
        String gdpPerCapitaConvertedFormatted,
        BigDecimal exchangeRate,
        String exchangeRateDate
) {
}
