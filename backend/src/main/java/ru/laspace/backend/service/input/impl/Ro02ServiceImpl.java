package ru.laspace.backend.service.input.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.input.ro02.Ro02DataResponse;
import ru.laspace.backend.dto.input.ro02.Ro02Dto;
import ru.laspace.backend.repository.input.Ro02Repository;
import ru.laspace.backend.service.input.Ro02Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class Ro02ServiceImpl implements Ro02Service {
    private final Ro02Repository ro02Repository;

    @Override
    @Transactional(readOnly = true)
    public Ro02DataResponse getRo02Data(LocalDate date) {
        List<Ro02Dto> rotationRecords = ro02Repository.findByDateInRange(date);

        if (rotationRecords == null || rotationRecords.isEmpty()) {
            log.info("Данные сезонных разворотов не найдены для даты: {}", date);
            return null;
        }

        return Ro02DataResponse.builder()
                .rotations(rotationRecords)
                .build();
    }
}
