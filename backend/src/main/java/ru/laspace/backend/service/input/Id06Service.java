package ru.laspace.backend.service.input;

import java.time.LocalDate;

import ru.laspace.backend.dto.input.id06.Id06DataResponse;

public interface Id06Service {
    Id06DataResponse getOperatorData(LocalDate date);
}
