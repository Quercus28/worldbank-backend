package com.udea.worldbank.service;

import com.udea.worldbank.dto.GdpPerCapitaResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.ExchangeRateApiException;
import com.udea.worldbank.exception.WorldBankApiException;

/**
 * Caso de uso: PIB per capita (indicador derivado que combina PIB y poblacion).
 */
public interface CalculateGdpPerCapitaUseCase {

    /**
     * @param targetCurrency moneda (codigo ISO 4217, p.ej. "EUR") a la que tambien se
     *                       quiere convertir el PIB per capita, o {@code null}/vacio para omitirlo
     * @throws DataNotFoundException    si falta el PIB o la poblacion del año pedido,
     *                                  o {@code targetCurrency} no esta soportada
     * @throws WorldBankApiException    si falla la comunicacion con la API externa del Banco Mundial
     * @throws ExchangeRateApiException si falla la comunicacion con la API externa de tasas de cambio
     */
    GdpPerCapitaResponse calculateGdpPerCapita(String countryIso3, int year, String targetCurrency);
}
