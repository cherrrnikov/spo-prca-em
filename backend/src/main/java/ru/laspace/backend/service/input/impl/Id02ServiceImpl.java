package ru.laspace.backend.service.input.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.input.id02.Id02Dto;
import ru.laspace.backend.repository.input.Id02Repository;
import ru.laspace.backend.service.input.Id02Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class Id02ServiceImpl implements Id02Service {
    private final Id02Repository id02Repository;

    @Override
    @Transactional(readOnly = true)
    public Id02Dto getBortData(LocalDate date) {
        log.info("Запрос состояния бортовых систем для даты: {}", date);

        try {
            Id02Dto bortData = id02Repository.findLatestByDate(date);

            if (bortData == null) {
                log.info("Данные о состоянии бортовых систем не найдены для даты: {}", date);
                return null;
            }

            log.info("Найдена запись ИД02 ID={}, КА={}, RNF={}",
                    bortData.getId(),
                    bortData.getNKa(),
                    bortData.getRnf());

            return bortData;

        } catch (Exception e) {
            log.error("Ошибка при получении данных ИД02 для даты {}: {}",
                    date, e.getMessage(), e);
            throw e;
        }
    }

}
