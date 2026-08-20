package com.udea.worldbank.service;

import com.udea.worldbank.client.CountriesWorldBankClient;
import com.udea.worldbank.client.ObservationsWorldBankClient;
import com.udea.worldbank.dto.RankingItemResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.WorldBankApiException;
import com.udea.worldbank.model.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * CASO DE USO: ranking de paises por indicador.
 *   - pide el indicador para "all"
 *   - descarta valores nulos
 *   - descarta agregados regionales (cruza con la lista real de paises)
 *   - ordena de mayor a menor y recorta al limite
 */
@Service
public final class RankingService implements RankingUseCase {

    private static final Logger log = LoggerFactory.getLogger(RankingService.class);
    private static final String ALL_COUNTRIES = "all";

    private final ObservationsWorldBankClient observationsClient;
    private final CountriesWorldBankClient countriesClient;

    public RankingService(ObservationsWorldBankClient observationsClient,
                          CountriesWorldBankClient countriesClient) {
        this.observationsClient = observationsClient;
        this.countriesClient = countriesClient;
    }

    @Override
    public List<RankingItemResponse> ranking(String indicator, int year, int limit) {
        List<Observation> observations = getGlobalObservations(indicator, year);
        Set<String> realCountryCodes = getRealCountries();

        List<Observation> sorted = observations.stream()
                .filter(o -> o.value() != null)
                .filter(o -> realCountryCodes.contains(o.countryIso3()))
                .sorted(Comparator.comparingDouble(Observation::value).reversed())
                .limit(limit)
                .toList();

        if (sorted.isEmpty()) {
            throw new DataNotFoundException(
                    "No data found for '%s' in %d".formatted(indicator, year));
        }

        return IntStream.range(0, sorted.size())
                .mapToObj(i -> {
                    Observation o = sorted.get(i);
                    return new RankingItemResponse(i + 1, o.countryIso3(), o.countryName(), o.value());
                })
                .toList();
    }

    private List<Observation> getGlobalObservations(String indicator, int year) {
        try {
            return observationsClient.getObservations(ALL_COUNTRIES, indicator, year, year);
        } catch (WorldBankApiException e) {
            log.error("Could not fetch '{}' for all countries in {}: {}",
                    indicator, year, e.getMessage());
            throw e;
        }
    }

    private Set<String> getRealCountries() {
        try {
            return countriesClient.getRealCountryCodes();
        } catch (WorldBankApiException e) {
            log.error("Could not fetch the list of real countries to build the ranking: {}",
                    e.getMessage());
            throw e;
        }
    }
}