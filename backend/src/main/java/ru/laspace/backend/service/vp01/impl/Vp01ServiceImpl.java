package ru.laspace.backend.service.vp01.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.entity.rn.RnVp01;
import ru.laspace.backend.repository.rn.RnVp01Repository;
import ru.laspace.backend.service.vp01.Vp01BuilderService;
import ru.laspace.backend.service.vp01.Vp01Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class Vp01ServiceImpl implements Vp01Service {

    private final Vp01BuilderService vp01BuilderService;
    private final RnVp01Repository rnVp01Repository;

    @Override
    @Transactional
    public String generateAndSave(Integer numRp, Integer numKa) {
        Integer maxRnf = rnVp01Repository.findMaxRnf();
        Integer rnf = maxRnf + 1;
        log.info("Генерация ВП01: numRp={}, numKa={}, rnf={}", numRp, numKa, rnf);

        String fo = vp01BuilderService.build(numRp, numKa, rnf);
        log.info("ВП01 сформирована:\n{}", fo);

        RnVp01 record = new RnVp01();
        record.setRnf(rnf);
        record.setDsf(LocalDateTime.now());
        record.setNRp(numRp);
        record.setNKa(numKa);
        record.setFo(fo);
        rnVp01Repository.save(record);

        log.info("ВП01 сохранена в БД с rnf={}", rnf);
        return fo;
    }
}
