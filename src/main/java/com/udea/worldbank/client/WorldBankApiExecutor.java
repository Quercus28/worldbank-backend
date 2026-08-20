package com.udea.worldbank.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.udea.worldbank.exception.WorldBankApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.function.Function;

/**
 * INFRAESTRUCTURA COMPARTIDA de acceso a la World Bank Indicators API.
 *
 * No representa un caso de uso: es el mecanismo comun que usan los distintos
 * clientes especializados para llamar a la API y validar su sobre de respuesta.
 *
 *  1. La API responde SIEMPRE un array de 2 posiciones: [ metadata, [datos] ].
 *     Aqui se extrae la posicion [1] y se maneja el caso de datos == null.
 *  2. Los errores de la API llegan como un array de 1 posicion con un objeto
 *     "message". Se detecta y se traduce a una excepcion propia.
 */
@Component
public class WorldBankApiExecutor {

    public static final int MAX_PAGE_SIZE = 20000;

    private final RestClient restClient;

    public WorldBankApiExecutor(RestClient worldBankRestClient) {
        this.restClient = worldBankRestClient;
    }

    public JsonNode execute(Function<UriBuilder, URI> uriFn) {
        try {
            return restClient.get()
                    .uri(uriFn::apply)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new WorldBankApiException(
                    "Failed to call the World Bank API: " + e.getMessage(), e);
        }
    }

    /**
     * Valida la envoltura de la respuesta y devuelve el nodo de datos (posicion
     * [1]) o null si la consulta no devolvio datos.
     */
    public JsonNode extractData(JsonNode root) {
        if (root == null || !root.isArray() || root.isEmpty()) {
            throw new WorldBankApiException("Unexpected response format from the API");
        }
        JsonNode metadata = root.get(0);
        if (metadata.has("message")) {
            throw new WorldBankApiException("The API returned an error: " + metadata.get("message"));
        }
        if (root.size() < 2 || root.get(1).isNull()) {
            return null; // consulta valida pero sin datos
        }
        return root.get(1);
    }
}