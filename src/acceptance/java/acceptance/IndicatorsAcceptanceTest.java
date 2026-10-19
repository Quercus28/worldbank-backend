package acceptance;

import com.intuit.karate.junit5.Karate;

class IndicatorsAcceptanceTest {
    @Karate.Test
    Karate indicators() {
        return Karate.run("classpath:acceptance/indicators.feature");
    }
}
