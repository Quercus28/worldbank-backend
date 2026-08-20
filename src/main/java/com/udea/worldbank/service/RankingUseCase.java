package com.udea.worldbank.service;

import com.udea.worldbank.dto.RankingItemResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.WorldBankApiException;

import java.util.List;

/**
 * Caso de uso: ranking de paises por un indicador en un año dado.
 */
public interface RankingUseCase {

    /**
     * @throws DataNotFoundException si no hay paises reales con valor para el año pedido
     * @throws WorldBankApiException si falla la comunicacion con la API externa
     */
    List<RankingItemResponse> ranking(String indicator, int year, int limit);
}
