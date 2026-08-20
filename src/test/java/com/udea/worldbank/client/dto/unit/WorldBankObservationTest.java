package com.udea.worldbank.client.dto.unit;

import com.udea.worldbank.client.dto.WorldBankObservation;
import com.udea.worldbank.client.dto.WorldBankRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class WorldBankObservationTest {

    @Test
    void test_accessors_returnValuesUsedInConstruction() {
        // Arrange
        WorldBankRef indicator = new WorldBankRef("SP.POP.TOTL", "Population, total");
        WorldBankRef country = new WorldBankRef("BR", "Brazil");

        // Act
        WorldBankObservation obs = new WorldBankObservation(
                indicator, country, "BRA", "2020", 212559409.0, "unit", "status", 0);

        // Assert
        assertThat(obs.indicator()).isEqualTo(indicator);
        assertThat(obs.country()).isEqualTo(country);
        assertThat(obs.countryIso3Code()).isEqualTo("BRA");
        assertThat(obs.date()).isEqualTo("2020");
        assertThat(obs.value()).isEqualTo(212559409.0);
        assertThat(obs.unit()).isEqualTo("unit");
        assertThat(obs.obsStatus()).isEqualTo("status");
        assertThat(obs.decimal()).isEqualTo(0);
    }
}
