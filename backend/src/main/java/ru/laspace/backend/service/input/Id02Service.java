package ru.laspace.backend.service.input;

import java.time.LocalDate;

import ru.laspace.backend.dto.input.id02.Id02Dto;

public interface Id02Service {
    Id02Dto getBortData(LocalDate date);
}
