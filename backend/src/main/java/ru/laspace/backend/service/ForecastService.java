package ru.laspace.backend.service;

import java.time.LocalDate;

import ru.laspace.backend.dto.forecast.ForecastDataResponse;

public interface ForecastService {
    ForecastDataResponse getOperatorData(LocalDate date);
}
