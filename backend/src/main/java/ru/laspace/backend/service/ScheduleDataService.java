package ru.laspace.backend.service;

import java.time.LocalDate;

import ru.laspace.backend.dto.id06.Id06DataResponse;

public interface ScheduleDataService {
    Id06DataResponse getOperatorData(LocalDate date);
}
