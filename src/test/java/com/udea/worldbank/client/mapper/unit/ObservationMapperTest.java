package com.udea.worldbank.client.mapper.unit;

import com.udea.worldbank.client.dto.WorldBankObservation;
import com.udea.worldbank.client.dto.WorldBankRef;
import com.udea.worldbank.client.mapper.ObservationMapper;
import com.udea.worldbank.model.Observation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ObservationMapperTest {

    private final ObservationMapper mapper = new ObservationMapper();

    @Test
    void test_toDomain_mapsAllFields_whenCountryAndIndicatorArePresent() {
        // Arrange
        WorldBankObservation raw = new WorldBankObservation(
                new WorldBankRef("SP.POP.TOTL", "Population, total"),
                new WorldBankRef("BR", "Brazil"),
                "BRA", "2020", 212559409.0, "", "", 0);

        // Act
        Observation result = mapper.toDomain(raw);

        // Assert
        assertThat(result.countryId()).isEqualTo("BR");
        assertThat(result.countryName()).isEqualTo("Brazil");
        assertThat(result.countryIso3()).isEqualTo("BRA");
        assertThat(result.indicatorId()).isEqualTo("SP.POP.TOTL");
        assertThat(result.indicatorName()).isEqualTo("Population, total");
        assertThat(result.year()).isEqualTo(2020);
        assertThat(result.value()).isEqualTo(212559409.0);
    }

    @Test
    void test_toDomain_leavesCountryAndIndicatorFieldsNull_whenAbsentFromTheResponse() {
        // Arrange
        WorldBankObservation raw = new WorldBankObservation(
                null, null, "BRA", "2020", null, "", "", 0);

        // Act
        Observation result = mapper.toDomain(raw);

        // Assert
        assertThat(result.countryId()).isNull();
        assertThat(result.countryName()).isNull();
        assertThat(result.indicatorId()).isNull();
        assertThat(result.indicatorName()).isNull();
        assertThat(result.countryIso3()).isEqualTo("BRA");
        assertThat(result.value()).isNull();
    }

    @Test
    void test_toDomain_returnsMinusOne_whenDateIsNotAFourDigitYear() {
        // Arrange
        WorldBankObservation raw = new WorldBankObservation(
                new WorldBankRef("SP.POP.TOTL", "Population, total"),
                new WorldBankRef("BR", "Brazil"),
                "BRA", "2020M01", 1.0, "", "", 0);

        // Act
        Observation result = mapper.toDomain(raw);

        // Assert
        assertThat(result.year()).isEqualTo(-1);
    }
}
