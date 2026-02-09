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
import ru.laspace.backend.dto.id06.Id06OnaDto;
import ru.laspace.backend.dto.id06.Id06TnpDto;
import ru.laspace.backend.dto.id06.Id06TsDto;
import ru.laspace.backend.repository.Id06Repository;
import ru.laspace.backend.service.ScheduleDataService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleDataServiceImpl implements ScheduleDataService {
    private final Id06Repository id06Repository;

    @Override
    @Transactional(readOnly = true)
    public Id06DataResponse getOperatorData(LocalDate date) {
        log.info("Запрос данных оператора для даты: {}", date);

        Id06MainDto mainRecord = id06Repository.findLatestByDate(date);

        if (mainRecord == null) {
            log.info("Данные не найдены для даты: {}", date);
            return null;
        }

        log.info("Найдена запись id06 ID={}, KA={}", mainRecord.getId(), mainRecord.getNKa());

        List<Id06KvdDto> kvdRecords = id06Repository.findKvdByMainId(mainRecord.getId());
        List<Id06TnpDto> tnpRecords = id06Repository.findTnpByMainId(mainRecord.getId());
        List<Id06TsDto> tsRecords = id06Repository.findTsByMainId(mainRecord.getId());
        List<Id06OnaDto> onaRecords = id06Repository.findOnaByMainId(mainRecord.getId());

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
