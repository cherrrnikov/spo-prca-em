package ru.laspace.backend.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.repository.programs.ProgramsMainRepository;
import ru.laspace.backend.service.ProgramNumberService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProgramNumberServiceImpl implements ProgramNumberService {

    private final ProgramsMainRepository programsMainRepository;

    @Override
    @Transactional(readOnly = true)
    public Integer generateNextProgramNumber(Integer numKa) {
        // Ищем максимальный numRp для данного numKa
        Integer maxNumRp = programsMainRepository.findMaxNumRpByNumKa(numKa);

        // Если записей нет, начинаем с 1
        Integer nextNumber = (maxNumRp == null) ? 1 : maxNumRp + 1;

        log.info("Сгенерирован номер ПРЦА для КА {}: {}", numKa, nextNumber);

        return nextNumber;
    }
}