package com.udea.worldbank.client.dto.unit;

import com.udea.worldbank.client.dto.WorldBankRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class WorldBankRefTest {

    @Test
    void test_accessors_returnValuesUsedInConstruction() {
        // Arrange & Act
        WorldBankRef ref = new WorldBankRef("BR", "Brazil");

        // Assert
        assertThat(ref.id()).isEqualTo("BR");
        assertThat(ref.value()).isEqualTo("Brazil");
    }
}
