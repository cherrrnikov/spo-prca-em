package ru.laspace.backend.service;

import java.time.LocalDate;

import ru.laspace.backend.dto.schedule.Id06DataResponse;

public interface ScheduleDataService {
    Id06DataResponse getOperatorData(LocalDate date);
}
