package ru.laspace.backend.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.kr01.Kr01DataResponse;
import ru.laspace.backend.dto.kr01.Kr01ImpulseDto;
import ru.laspace.backend.dto.kr01.Kr01MainDto;
import ru.laspace.backend.repository.Kr01Repository;
import ru.laspace.backend.service.Kr01Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class Kr01ServiceImpl implements Kr01Service {
    private final Kr01Repository kr01Repository;

    @Override
    @Transactional(readOnly = true)
    public Kr01DataResponse getKr01Data(LocalDate date) {
        Kr01MainDto mainRecord = kr01Repository.findLatestByDate(date);

        if (mainRecord == null) {
            log.info("Данные коррекции орбиты не найдены для даты: {}", date);
            return null;
        }

        log.info("Найдена запись kr01_main ID={}, KA={},RNF={}", mainRecord.getId(), mainRecord.getNKa(),
                mainRecord.getRnf());

        List<Kr01ImpulseDto> impulseRecords = kr01Repository.findKr01ImpulseByMainId(mainRecord.getId());

        return Kr01DataResponse.builder()
                .main(mainRecord)
                .impulses(impulseRecords)
                .build();
    }
}
