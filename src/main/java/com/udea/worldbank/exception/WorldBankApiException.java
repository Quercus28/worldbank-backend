package com.udea.worldbank.exception;

/** Error al comunicarse con la API externa o al interpretar su respuesta. */
public class WorldBankApiException extends RuntimeException {

    public WorldBankApiException(String message) {
        super(message);
    }

    public WorldBankApiException(String message, Throwable cause) {
        super(message, cause);
    }
}