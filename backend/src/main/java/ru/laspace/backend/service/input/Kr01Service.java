package ru.laspace.backend.service.input;

import java.time.LocalDate;

import ru.laspace.backend.dto.input.kr01.Kr01DataResponse;

public interface Kr01Service {
    // Поиск по дате приложения импульса date_im в kr01
    Kr01DataResponse getKr01Data(LocalDate date);
}
