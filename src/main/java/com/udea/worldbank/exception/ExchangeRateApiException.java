package com.udea.worldbank.exception;

/** Error al comunicarse con la API externa de tasas de cambio o al interpretar su respuesta. */
public class ExchangeRateApiException extends RuntimeException {

    public ExchangeRateApiException(String message) {
        super(message);
    }

    public ExchangeRateApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
