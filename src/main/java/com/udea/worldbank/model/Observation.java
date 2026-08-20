package com.udea.worldbank.model;

/**
 * Modelo de DOMINIO de la aplicacion. Es independiente del formato que
 * devuelve la API externa: la capa de acceso a datos traduce el JSON crudo
 * del Banco Mundial a este tipo, de modo que el resto de la aplicacion nunca
 * depende de la estructura ajena.
 *
 * @param countryId      codigo ISO2 del pais (p.ej. "BR")
 * @param countryName    nombre del pais (p.ej. "Brazil")
 * @param countryIso3    codigo ISO3 del pais (p.ej. "BRA")
 * @param indicatorId    codigo del indicador (p.ej. "SP.POP.TOTL")
 * @param indicatorName  nombre legible del indicador
 * @param year           año de la observacion
 * @param value          valor del indicador; puede ser null si el pais no reporto ese año
 */
public record Observation(
        String countryId,
        String countryName,
        String countryIso3,
        String indicatorId,
        String indicatorName,
        int year,
        Double value
) {
}