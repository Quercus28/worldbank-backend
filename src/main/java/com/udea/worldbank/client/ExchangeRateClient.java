package com.udea.worldbank.client;

import com.udea.worldbank.client.dto.FrankfurterLatestRates;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.ExchangeRateApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * INFRAESTRUCTURA de acceso a la Frankfurter API (tasas de cambio de
 * referencia del Banco Central Europeo, publica y sin autenticacion).
 *
 * A diferencia de la World Bank API, la respuesta no viene envuelta en un
 * array de 2 posiciones: es un objeto JSON plano, asi que no hace falta un
 * ejecutor compartido con logica de "sobre".
 */
@Component
public class ExchangeRateClient {

    private final RestClient restClient;

    public ExchangeRateClient(RestClient frankfurterRestClient) {
        this.restClient = frankfurterRestClient;
    }

    /**
     * @param from moneda de origen (codigo ISO 4217, p.ej. "USD")
     * @param to   moneda de destino (codigo ISO 4217, p.ej. "EUR")
     * @return la tasa de cambio mas reciente publicada de {@code from} a {@code to}
     * @throws DataNotFoundException    si alguna de las monedas no esta soportada, o el par es invalido
     * @throws ExchangeRateApiException si falla la comunicacion con la API
     */
    public FrankfurterLatestRates getLatestRate(String from, String to) {
        try {
            FrankfurterLatestRates response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/latest")
                            .queryParam("base", from)
                            .queryParam("symbols", to)
                            .build())
                    .retrieve()
                    .body(FrankfurterLatestRates.class);

            if (response == null || response.rates() == null || !response.rates().containsKey(to)) {
                throw new DataNotFoundException(
                        "No exchange rate found from '%s' to '%s'".formatted(from, to));
            }
            return response;
        } catch (HttpClientErrorException e) {
            throw new DataNotFoundException(
                    "Unsupported currency or invalid pair: '%s' -> '%s'".formatted(from, to));
        } catch (RestClientException e) {
            throw new ExchangeRateApiException(
                    "Failed to call the exchange rate API: " + e.getMessage(), e);
        }
    }
}
