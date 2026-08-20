package com.udea.worldbank.service;

import com.udea.worldbank.client.ExchangeRateClient;
import com.udea.worldbank.client.dto.FrankfurterLatestRates;
import com.udea.worldbank.dto.ExchangeRateResponse;
import com.udea.worldbank.exception.ExchangeRateApiException;
import com.udea.worldbank.util.CurrencyFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CASO DE USO: convertir un monto entre dos monedas.
 *   - si origen y destino son la misma moneda, no hace falta llamar a la API externa
 *   - si no, pide la tasa mas reciente a Frankfurter y calcula el monto convertido
 */
@Service
public final class ExchangeRateService implements ExchangeRateUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final ExchangeRateClient exchangeRateClient;

    public ExchangeRateService(ExchangeRateClient exchangeRateClient) {
        this.exchangeRateClient = exchangeRateClient;
    }

    @Override
    public ExchangeRateResponse convert(String from, String to, BigDecimal amount) {
        String fromCurrency = from.toUpperCase();
        String toCurrency = to.toUpperCase();

        if (fromCurrency.equals(toCurrency)) {
            BigDecimal converted = CurrencyFormatter.round(amount, toCurrency);
            return new ExchangeRateResponse(
                    fromCurrency, toCurrency, BigDecimal.ONE, amount, converted,
                    CurrencyFormatter.format(converted, toCurrency), LocalDate.now().toString());
        }

        FrankfurterLatestRates latest = getLatestRate(fromCurrency, toCurrency);
        BigDecimal rate = latest.rates().get(toCurrency);
        BigDecimal converted = CurrencyFormatter.round(amount.multiply(rate), toCurrency);

        return new ExchangeRateResponse(
                fromCurrency, toCurrency, rate, amount, converted,
                CurrencyFormatter.format(converted, toCurrency), latest.date());
    }

    /**
     * Aisla la llamada externa para poder registrar el contexto de negocio
     * (par de monedas) que la capa de acceso a datos desconoce, antes de dejar
     * que {@link com.udea.worldbank.exception.GlobalExceptionHandler} la
     * traduzca a una respuesta HTTP.
     */
    private FrankfurterLatestRates getLatestRate(String from, String to) {
        try {
            return exchangeRateClient.getLatestRate(from, to);
        } catch (ExchangeRateApiException e) {
            log.error("Could not fetch the exchange rate from '{}' to '{}': {}", from, to, e.getMessage());
            throw e;
        }
    }
}
