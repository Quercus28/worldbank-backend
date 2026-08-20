package com.udea.worldbank.dto;

import java.util.List;

/** Serie temporal limpia (sin nulos, ordenada cronologicamente). */
public record SeriesResponse(
        String countryIso3,
        String countryName,
        String indicatorId,
        String indicatorName,
        List<SeriesPointResponse> points
) {
}
