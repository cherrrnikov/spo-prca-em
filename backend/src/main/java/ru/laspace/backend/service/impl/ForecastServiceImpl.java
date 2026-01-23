package ru.laspace.backend.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.forecast.ForecastDataResponse;
import ru.laspace.backend.dto.forecast.ForecastDto;
import ru.laspace.backend.dto.forecast.ShadowDto;
import ru.laspace.backend.dto.forecast.ZasvetkaDto;
import ru.laspace.backend.repository.ForecastRepository;
import ru.laspace.backend.service.ForecastService;

@Service
@Slf4j
@RequiredArgsConstructor

public class ForecastServiceImpl implements ForecastService {
    private ForecastRepository forecastRepository;

    @Override
    @Transactional(readOnly = true)
    public ForecastDataResponse getOperatorData(LocalDate date) {
        ForecastDto forecast = forecastRepository.findLatestByDate(date);

        if (forecast == null) {
            log.info("Прогноз не найден на дату {}", date);
            return null;
        }

        List<ShadowDto> shadows = forecastRepository.findShadowsByForecastId(forecast.getId());
        List<ZasvetkaDto> zasvetki = forecastRepository.findZasvetkiByForecastId(forecast.getId());

        log.info("Найдено теней: {}, засветок: {}", shadows.size(), zasvetki.size());

        return ForecastDataResponse.builder()
                .forecast(forecast)
                .shadows(shadows)
                .zasvetki(zasvetki)
                .build();
    }

}
