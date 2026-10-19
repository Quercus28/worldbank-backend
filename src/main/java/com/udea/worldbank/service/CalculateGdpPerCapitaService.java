package com.udea.worldbank.service;

import com.udea.worldbank.client.ExchangeRateClient;
import com.udea.worldbank.client.ObservationsWorldBankClient;
import com.udea.worldbank.client.dto.FrankfurterLatestRates;
import com.udea.worldbank.dto.GdpPerCapitaResponse;
import com.udea.worldbank.exception.DataNotFoundException;
import com.udea.worldbank.exception.ExchangeRateApiException;
import com.udea.worldbank.exception.WorldBankApiException;
import com.udea.worldbank.model.Observation;
import com.udea.worldbank.util.CurrencyFormatter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * CASO DE USO: PIB per capita (indicador derivado que el Banco Mundial no expone directamente).
 *   - pide PIB (NY.GDP.MKTP.CD) y poblacion (SP.POP.TOTL) del mismo pais y año
 *   - calcula PIB / poblacion, redondeado a 2 decimales
 *   - formatea ambos montos como moneda (USD)
 *   - opcionalmente, convierte el PIB per capita a otra moneda (Frankfurter API)
 */
@Service
public final class CalculateGdpPerCapitaService implements CalculateGdpPerCapitaUseCase {

    private static final String GDP_INDICATOR = "NY.GDP.MKTP.CD";
    private static final String POPULATION_INDICATOR = "SP.POP.TOTL";
    private static final String USD = "USD";
    private static final int USD_SCALE = 2;

    private final ObservationsWorldBankClient observationsClient;
    private final ExchangeRateClient exchangeRateClient;

    public CalculateGdpPerCapitaService(ObservationsWorldBankClient observationsClient,
                                        ExchangeRateClient exchangeRateClient) {
        this.observationsClient = observationsClient;
        this.exchangeRateClient = exchangeRateClient;
    }

    @Override
    public GdpPerCapitaResponse calculateGdpPerCapita(String countryIso3, int year, String targetCurrency) {
        Double gdp = pointValue(countryIso3, GDP_INDICATOR, year);
        Double population = pointValue(countryIso3, POPULATION_INDICATOR, year);

        if (gdp == null || population == null || population == 0) {
            throw new DataNotFoundException(
                    "Missing GDP or population data for '%s' in %d".formatted(countryIso3, year));
        }

        long populationAmount = population.longValue();
        BigDecimal gdpAmount = BigDecimal.valueOf(gdp).setScale(USD_SCALE, RoundingMode.HALF_UP);
        BigDecimal perCapitaAmount = BigDecimal.valueOf(gdp)
                .divide(BigDecimal.valueOf(populationAmount), USD_SCALE, RoundingMode.HALF_UP);

        Conversion conversion = convertIfRequested(perCapitaAmount, targetCurrency);

        return new GdpPerCapitaResponse(
                countryIso3.toUpperCase(),
                year,
                gdpAmount,
                formatUsd(gdpAmount),
                populationAmount,
                perCapitaAmount,
                formatUsd(perCapitaAmount),
                conversion.currency(),
                conversion.amount(),
                conversion.formatted(),
                conversion.rate(),
                conversion.rateDate());
    }

    /**
     * Convierte el PIB per capita (siempre en USD) a {@code targetCurrency} si se pidio.
     * Aisla la llamada externa para registrar el contexto de negocio antes de relanzar.
     */
    private Conversion convertIfRequested(BigDecimal perCapitaUsd, String targetCurrency) {
        if (targetCurrency == null || targetCurrency.isBlank()) {
            return Conversion.EMPTY;
        }
        String currency = targetCurrency.toUpperCase();
        FrankfurterLatestRates latest = getLatestRate(currency);
        BigDecimal rate = latest.rates().get(currency);
        BigDecimal converted = CurrencyFormatter.round(perCapitaUsd.multiply(rate), currency);
        return new Conversion(currency, converted, CurrencyFormatter.format(converted, currency), rate, latest.date());
    }

    private FrankfurterLatestRates getLatestRate(String targetCurrency) {
        try {
            return exchangeRateClient.getLatestRate(USD, targetCurrency);
        } catch (ExchangeRateApiException e) {
            throw new ExchangeRateApiException(
                    "Could not fetch the exchange rate to '%s': %s".formatted(targetCurrency, e.getMessage()), e);
        }
    }

    /**
     * Aisla la llamada externa para poder registrar el contexto de negocio
     * (pais, indicador, año) que la capa de acceso a datos desconoce, antes
     * de dejar que {@link com.udea.worldbank.exception.GlobalExceptionHandler}
     * la traduzca a una respuesta HTTP.
     *
     * @return el valor del indicador para ese año, o {@code null} si el Banco Mundial no lo reporta
     */
    private Double pointValue(String countryIso3, String indicator, int year) {
        try {
            return observationsClient.getObservations(countryIso3, indicator, year, year).stream()
                    .filter(o -> o.value() != null)
                    .map(Observation::value)
                    .findFirst()
                    .orElse(null);
        } catch (WorldBankApiException e) {
            throw new WorldBankApiException(
                    "Could not fetch '%s' for '%s' in %d: %s".formatted(indicator, countryIso3, year, e.getMessage()), e);
        }
    }

    private String formatUsd(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    private record Conversion(String currency, BigDecimal amount, String formatted, BigDecimal rate, String rateDate) {
        private static final Conversion EMPTY = new Conversion(null, null, null, null, null);
    }
}
