package com.udea.worldbank.service;

import com.udea.worldbank.client.ObservationsWorldBankClient;
import com.udea.worldbank.dto.SeriesPointResponse;
import com.udea.worldbank.dto.SeriesResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.WorldBankApiException;
import com.udea.worldbank.model.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * CASO DE USO: serie temporal limpia de un indicador para un pais.
 *   - descarta años sin dato (value == null)
 *   - reordena cronologicamente (la API los devuelve del mas nuevo al mas viejo)
 *   - mapea a un DTO propio
 */
@Service
public final class GetSeriesService implements GetSeriesUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetSeriesService.class);

    private final ObservationsWorldBankClient observationsClient;

    public GetSeriesService(ObservationsWorldBankClient observationsClient) {
        this.observationsClient = observationsClient;
    }

    @Override
    public SeriesResponse getSeries(String countryIso3, String indicator, Integer from, Integer to) {
        List<Observation> observations = getObservations(countryIso3, indicator, from, to);

        List<SeriesPointResponse> points = observations.stream()
                .filter(o -> o.value() != null)
                .sorted(Comparator.comparingInt(Observation::year))
                .map(o -> new SeriesPointResponse(o.year(), o.value()))
                .toList();

        if (points.isEmpty()) {
            throw new DataNotFoundException(
                    "No data found for '%s' in '%s' within the requested range".formatted(indicator, countryIso3));
        }

        Observation reference = observations.get(0);
        return new SeriesResponse(
                countryIso3.toUpperCase(),
                reference.countryName(),
                indicator,
                reference.indicatorName(),
                points);
    }

    /**
     * Aisla la llamada externa para poder registrar el contexto de negocio
     * (pais, indicador, rango) que la capa de acceso a datos desconoce, antes
     * de dejar que {@link com.udea.worldbank.exception.GlobalExceptionHandler}
     * la traduzca a una respuesta HTTP.
     */
    private List<Observation> getObservations(String countryIso3, String indicator, Integer from, Integer to) {
        try {
            return observationsClient.getObservations(countryIso3, indicator, from, to);
        } catch (WorldBankApiException e) {
            log.error("Could not fetch the series for '{}' in '{}' (range {}-{}): {}",
                    indicator, countryIso3, from, to, e.getMessage());
            throw e;
        }
    }
}
