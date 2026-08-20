package com.udea.worldbank.dto;

import java.math.BigDecimal;

/**
 * Resultado de convertir un monto entre dos monedas usando la tasa de
 * referencia mas reciente (Banco Central Europeo, via Frankfurter).
 *
 * @param rate                    tasa de cambio de 1 {@code from} a {@code to}
 * @param convertedAmount         {@code amount} convertido a {@code to}
 * @param convertedAmountFormatted el mismo monto convertido, formateado como moneda
 * @param rateDate                fecha de publicacion de la tasa usada
 */
public record ExchangeRateResponse(
        String from,
        String to,
        BigDecimal rate,
        BigDecimal amount,
        BigDecimal convertedAmount,
        String convertedAmountFormatted,
        String rateDate
) {
}
