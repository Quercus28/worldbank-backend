package com.udea.worldbank.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Redondea y formatea montos en una moneda ISO 4217 arbitraria (a diferencia
 * de un formateo fijo a USD). La usan los casos de uso que convierten
 * indicadores del Banco Mundial (siempre en USD) a otra moneda via
 * {@link com.udea.worldbank.client.ExchangeRateClient}.
 */
public final class CurrencyFormatter {

    private CurrencyFormatter() {
    }

    /** Redondea al numero de decimales que usa oficialmente la moneda (2 si no esta definido). */
    public static BigDecimal round(BigDecimal amount, String currencyCode) {
        return amount.setScale(fractionDigits(currencyCode), RoundingMode.HALF_UP);
    }

    /** Formatea el monto como moneda, p.ej. "€85.61" para EUR o "¥12,345" para JPY. */
    public static String format(BigDecimal amount, String currencyCode) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);
        numberFormat.setCurrency(Currency.getInstance(currencyCode));
        return numberFormat.format(amount);
    }

    private static int fractionDigits(String currencyCode) {
        int digits = Currency.getInstance(currencyCode).getDefaultFractionDigits();
        return digits < 0 ? 2 : digits;
    }
}
