package com.udea.worldbank.dto;

/** Un punto de una serie temporal en la respuesta de la API propia. */
public record SeriesPointResponse(int year, double value) {
}