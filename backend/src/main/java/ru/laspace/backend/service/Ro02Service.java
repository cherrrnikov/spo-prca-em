package ru.laspace.backend.service;

import java.time.LocalDate;

import ru.laspace.backend.dto.ro02.Ro02DataResponse;

public interface Ro02Service {
    Ro02DataResponse getRo02Data(LocalDate date);
}
