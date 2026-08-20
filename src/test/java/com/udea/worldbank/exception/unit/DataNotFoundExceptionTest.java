package com.udea.worldbank.exception.unit;

import com.udea.worldbank.exception.DataNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class DataNotFoundExceptionTest {

    @Test
    void test_constructor_setsTheMessage() {
        // Arrange & Act
        DataNotFoundException ex = new DataNotFoundException("no data found");

        // Assert
        assertThat(ex.getMessage()).isEqualTo("no data found");
    }
}
