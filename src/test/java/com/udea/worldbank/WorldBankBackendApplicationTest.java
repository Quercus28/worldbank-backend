package com.udea.worldbank;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@Tag("unit")
class WorldBankBackendApplicationTest {

    @Test
    void test_constructor_createsAnInstance() {
        // Arrange & Act
        WorldBankBackendApplication app = new WorldBankBackendApplication();

        // Assert
        assertThat(app).isNotNull();
    }

    @Test
    void test_main_delegatesToSpringApplicationRun_withTheApplicationClassAndArguments() {
        // Arrange
        String[] args = {"--server.port=0"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            // Act
            WorldBankBackendApplication.main(args);

            // Assert
            springApplication.verify(() -> SpringApplication.run(WorldBankBackendApplication.class, args));
        }
    }
}