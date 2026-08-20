package com.udea.worldbank.controller;

import com.udea.worldbank.dto.GdpPerCapitaResponse;
import com.udea.worldbank.dto.RankingItemResponse;
import com.udea.worldbank.dto.SeriesResponse;
import com.udea.worldbank.service.CalculateGdpPerCapitaUseCase;
import com.udea.worldbank.service.GetSeriesUseCase;
import com.udea.worldbank.service.RankingUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CAPA WEB (presentacion). Solo traduce peticiones HTTP a llamadas de la capa
 * de servicio y devuelve DTOs propios. No contiene logica de negocio.
 */
@RestController
@RequestMapping("/api/indicators")
@Validated
public class IndicatorController {

    private final GetSeriesUseCase getSeriesUseCase;
    private final CalculateGdpPerCapitaUseCase calculateGdpPerCapitaUseCase;
    private final RankingUseCase rankingUseCase;

    public IndicatorController(GetSeriesUseCase getSeriesUseCase,
                               CalculateGdpPerCapitaUseCase calculateGdpPerCapitaUseCase,
                               RankingUseCase rankingUseCase) {
        this.getSeriesUseCase = getSeriesUseCase;
        this.calculateGdpPerCapitaUseCase = calculateGdpPerCapitaUseCase;
        this.rankingUseCase = rankingUseCase;
    }

    /**
     * Serie temporal de un indicador para un pais.
     * Ejemplo: /api/indicators/series?country=BRA&indicator=SP.POP.TOTL&from=2010&to=2020
     */
    @GetMapping("/series")
    public SeriesResponse series(
            @RequestParam @NotBlank String country,
            @RequestParam @NotBlank String indicator,
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to) {
        return getSeriesUseCase.getSeries(country, indicator, from, to);
    }

    /**
     * PIB per capita (indicador derivado).
     * Ejemplo: /api/indicators/gdp-per-capita?country=BRA&year=2022
     */
    @GetMapping("/gdp-per-capita")
    public GdpPerCapitaResponse gdpPerCapita(
            @RequestParam @NotBlank String country,
            @RequestParam @Min(1960) int year) {
        return calculateGdpPerCapitaUseCase.calculateGdpPerCapita(country, year);
    }

    /**
     * Ranking de paises por indicador.
     * Ejemplo: /api/indicators/ranking?indicator=NY.GDP.MKTP.CD&year=2022&limit=10
     */
    @GetMapping("/ranking")
    public List<RankingItemResponse> ranking(
            @RequestParam @NotBlank String indicator,
            @RequestParam @Min(1960) int year,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return rankingUseCase.ranking(indicator, year, limit);
    }
}