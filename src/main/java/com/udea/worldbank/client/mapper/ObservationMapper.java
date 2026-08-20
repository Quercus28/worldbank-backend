package com.udea.worldbank.client.mapper;

import com.udea.worldbank.client.dto.WorldBankObservation;
import com.udea.worldbank.model.Observation;
import org.springframework.stereotype.Component;

/**
 * Traduce un DTO crudo de la API externa ({@link WorldBankObservation}) al
 * modelo de dominio ({@link Observation}). Mantener esta traduccion aqui es lo
 * que evita que la estructura del Banco Mundial se filtre hacia el resto de la
 * aplicacion.
 */
@Component
public class ObservationMapper {

    public Observation toDomain(WorldBankObservation raw) {
        String countryId = raw.country() != null ? raw.country().id() : null;
        String countryName = raw.country() != null ? raw.country().value() : null;
        String indicatorId = raw.indicator() != null ? raw.indicator().id() : null;
        String indicatorName = raw.indicator() != null ? raw.indicator().value() : null;

        return new Observation(
                countryId,
                countryName,
                raw.countryIso3Code(),
                indicatorId,
                indicatorName,
                parseYear(raw.date()),
                raw.value()
        );
    }

    /**
     * Los indicadores usados aqui son anuales, asi que "date" es un año de 4
     * digitos. Si llegara otro formato (mensual/trimestral) se devuelve -1 para
     * que la capa de servicio pueda descartar la observacion.
     */
    private int parseYear(String date) {
        try {
            return Integer.parseInt(date);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}