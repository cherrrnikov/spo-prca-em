package ru.laspace.backend.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.id06.Id06DataResponse;
import ru.laspace.backend.dto.id06.Id06KvdDto;
import ru.laspace.backend.dto.id06.Id06MainDto;
import ru.laspace.backend.dto.id06.Id06TnpDto;
import ru.laspace.backend.dto.id06.Id06TsDto;
import ru.laspace.backend.dto.id06.Id06OnaDto;

import ru.laspace.backend.repository.Id06Repository;
import ru.laspace.backend.service.ScheduleDataService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleDataServiceImpl implements ScheduleDataService {
    private final Id06Repository id06repository;

    @Override
    @Transactional(readOnly = true)
    public Id06DataResponse getOperatorData(LocalDate date) {
        log.info("Запрос данных оператора для даты: {}", date);

        Id06MainDto mainRecord = id06repository.findLatestByDate(date);

        if (mainRecord == null) {
            log.info("Данные не найдены для даты: {}", date);
            return null;
        }

        log.info("Найдена запись id06 ID={}, KA={}", mainRecord.getId(), mainRecord.getNKa());

        List<Id06KvdDto> kvdRecords = id06repository.findKvdByMainId(mainRecord.getId());
        List<Id06TnpDto> tnpRecords = id06repository.findTnpByMainId(mainRecord.getId());
        List<Id06TsDto> tsRecords = id06repository.findTsByMainId(mainRecord.getId());
        List<Id06OnaDto> onaRecords = id06repository.findOnaByMainId(mainRecord.getId());

        log.info("Найдено записей: KVD={}, TNP={}, TS={}, ONA={}",
                kvdRecords.size(), tnpRecords.size(), tsRecords.size(), onaRecords.size());

        return Id06DataResponse.builder()
                .main(mainRecord)
                .kvdList(kvdRecords)
                .tnpList(tnpRecords)
                .tsList(tsRecords)
                .onaList(onaRecords)
                .build();
    }
}
