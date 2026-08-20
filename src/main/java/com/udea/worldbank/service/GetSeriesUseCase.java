package com.udea.worldbank.service;

import com.udea.worldbank.dto.SeriesResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.WorldBankApiException;

/**
 * Caso de uso: serie temporal limpia de un indicador para un pais.
 *
 * Aislar el contrato en una interfaz permite que la capa web (u otros
 * consumidores) dependan de una abstraccion en lugar de la implementacion
 * concreta (principio de inversion de dependencias).
 */
public interface GetSeriesUseCase {

    /**
     * @throws DataNotFoundException si no hay valores para el rango pedido
     * @throws WorldBankApiException si falla la comunicacion con la API externa
     */
    SeriesResponse getSeries(String countryIso3, String indicator, Integer from, Integer to);
}
