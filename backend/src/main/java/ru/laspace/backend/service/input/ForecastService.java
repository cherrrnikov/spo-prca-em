package ru.laspace.backend.service.input;

import java.time.LocalDate;

import ru.laspace.backend.dto.input.forecast.ForecastDataResponse;

public interface ForecastService {
    ForecastDataResponse getOperatorData(LocalDate date);
}
