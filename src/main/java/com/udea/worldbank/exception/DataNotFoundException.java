package com.udea.worldbank.exception;

/** No existen datos para la combinacion pais/indicador/año solicitada. */
public class DataNotFoundException extends RuntimeException {

    public DataNotFoundException(String message) {
        super(message);
    }
}
