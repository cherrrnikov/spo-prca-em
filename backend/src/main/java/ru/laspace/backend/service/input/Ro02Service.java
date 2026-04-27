package ru.laspace.backend.service.input;

import java.time.LocalDate;

import ru.laspace.backend.dto.input.ro02.Ro02DataResponse;

public interface Ro02Service {
    Ro02DataResponse getRo02Data(LocalDate date);
}
