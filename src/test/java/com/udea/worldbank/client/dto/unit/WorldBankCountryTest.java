package com.udea.worldbank.client.dto.unit;

import com.udea.worldbank.client.dto.WorldBankCountry;
import com.udea.worldbank.client.dto.WorldBankRegion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class WorldBankCountryTest {

    @Test
    void test_isAggregate_returnsTrue_whenRegionIsNull() {
        // Arrange
        WorldBankCountry country = new WorldBankCountry("XYZ", "XY", "Something", null);

        // Act
        boolean result = country.isAggregate();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void test_isAggregate_returnsTrue_whenRegionIdIsNull() {
        // Arrange
        WorldBankCountry country = new WorldBankCountry("XYZ", "XY", "Something", new WorldBankRegion(null, null, null));

        // Act
        boolean result = country.isAggregate();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void test_isAggregate_returnsTrue_whenRegionIdIsNA() {
        // Arrange
        WorldBankCountry country = new WorldBankCountry("WLD", "1W", "World",
                new WorldBankRegion("NA", "NA", "Aggregates"));

        // Act
        boolean result = country.isAggregate();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void test_isAggregate_returnsFalse_whenCountryHasARealRegion() {
        // Arrange
        WorldBankCountry country = new WorldBankCountry("BRA", "BR", "Brazil",
                new WorldBankRegion("LCN", "ZJ", "Latin America & Caribbean"));

        // Act
        boolean result = country.isAggregate();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void test_accessors_returnValuesUsedInConstruction() {
        // Arrange
        WorldBankRegion region = new WorldBankRegion("LCN", "ZJ", "Latin America & Caribbean");

        // Act
        WorldBankCountry country = new WorldBankCountry("BRA", "BR", "Brazil", region);

        // Assert
        assertThat(country.id()).isEqualTo("BRA");
        assertThat(country.iso2Code()).isEqualTo("BR");
        assertThat(country.name()).isEqualTo("Brazil");
        assertThat(country.region()).isEqualTo(region);
    }
}
