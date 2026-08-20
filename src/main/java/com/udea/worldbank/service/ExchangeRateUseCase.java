package com.udea.worldbank.service;

import com.udea.worldbank.dto.ExchangeRateResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.ExchangeRateApiException;

import java.math.BigDecimal;

/** Caso de uso: convertir un monto entre dos monedas con la tasa de cambio de referencia mas reciente. */
public interface ExchangeRateUseCase {

    /**
     * @throws DataNotFoundException    si alguna moneda no esta soportada o el par es invalido
     * @throws ExchangeRateApiException si falla la comunicacion con la API externa
     */
    ExchangeRateResponse convert(String from, String to, BigDecimal amount);
}
