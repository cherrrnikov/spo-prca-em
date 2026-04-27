package ru.laspace.backend.service;

import ru.laspace.backend.dto.programs.ProgramCreateRequest;

public interface ProgramsService {
    Integer saveProgram(ProgramCreateRequest request);
}
