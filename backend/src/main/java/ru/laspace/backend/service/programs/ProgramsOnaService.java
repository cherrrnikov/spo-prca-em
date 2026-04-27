package ru.laspace.backend.service.programs;

import ru.laspace.backend.dto.programs.ProgramCreateRequest;
import ru.laspace.backend.entity.programs.ProgramsMain;

public interface ProgramsOnaService {
    void saveOnaPrograms(ProgramsMain programsMain, ProgramCreateRequest request);
}
