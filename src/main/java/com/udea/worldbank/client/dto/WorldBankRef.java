package com.udea.worldbank.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * En el JSON del Banco Mundial tanto "indicator" como "country" llegan con la
 * forma {"id": "...", "value": "..."}. Este record los representa a ambos.
 * Es un DTO de la API EXTERNA: solo vive en la capa de acceso a datos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorldBankRef(String id, String value) {
}
