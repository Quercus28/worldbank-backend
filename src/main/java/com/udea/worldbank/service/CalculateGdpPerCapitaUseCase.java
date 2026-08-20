package com.udea.worldbank.service;

import com.udea.worldbank.dto.GdpPerCapitaResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.WorldBankApiException;

/**
 * Caso de uso: PIB per capita (indicador derivado que combina PIB y poblacion).
 */
public interface CalculateGdpPerCapitaUseCase {

    /**
     * @throws DataNotFoundException si falta el PIB o la poblacion del año pedido
     * @throws WorldBankApiException si falla la comunicacion con la API externa
     */
    GdpPerCapitaResponse calculateGdpPerCapita(String countryIso3, int year);
}
