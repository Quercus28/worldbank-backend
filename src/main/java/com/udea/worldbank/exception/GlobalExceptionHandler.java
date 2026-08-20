package com.udea.worldbank.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centraliza el manejo de errores para que la capa web devuelva respuestas
 * HTTP coherentes en lugar de trazas de pila.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** No hay datos para lo pedido -> 404 Not Found. */
    @ExceptionHandler(DataNotFoundException.class)
    public ProblemDetail handleNotFound(DataNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Fallo hablando con la API externa -> 502 Bad Gateway. */
    @ExceptionHandler(WorldBankApiException.class)
    public ProblemDetail handleExternalApiError(WorldBankApiException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    /** Parametros invalidos (violan las restricciones del controlador) -> 400. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleValidationError(ConstraintViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}