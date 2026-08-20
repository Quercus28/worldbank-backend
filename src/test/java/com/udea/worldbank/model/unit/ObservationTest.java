package com.udea.worldbank.model.unit;

import com.udea.worldbank.model.Observation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ObservationTest {

    @Test
    void test_accessors_returnValuesUsedInConstruction() {
        // Arrange & Act
        Observation obs = new Observation(
                "BR", "Brazil", "BRA", "SP.POP.TOTL", "Population, total", 2020, 212559409.0);

        // Assert
        assertThat(obs.countryId()).isEqualTo("BR");
        assertThat(obs.countryName()).isEqualTo("Brazil");
        assertThat(obs.countryIso3()).isEqualTo("BRA");
        assertThat(obs.indicatorId()).isEqualTo("SP.POP.TOTL");
        assertThat(obs.indicatorName()).isEqualTo("Population, total");
        assertThat(obs.year()).isEqualTo(2020);
        assertThat(obs.value()).isEqualTo(212559409.0);
    }
}
