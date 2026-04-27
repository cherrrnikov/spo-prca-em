package ru.laspace.backend.service.pr.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.entity.rn.RnPr04;
import ru.laspace.backend.repository.rn.RnPr04Repository;
import ru.laspace.backend.service.pr.Pr04BuilderService;
import ru.laspace.backend.service.pr.Pr04Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class Pr04ServiceImpl implements Pr04Service {

    private final Pr04BuilderService pr04BuilderService;
    private final RnPr04Repository rnPr04Repository;

    @Override
    @Transactional
    public String generateAndSave(Integer numRp, Integer numKa) {
        Integer maxRnf = rnPr04Repository.findMaxRnf();
        Integer rnf = maxRnf + 1;
        log.info("Генерация ПР04: numRp={}, numKa={}, rnf={}", numRp, numKa, rnf);

        String fo = pr04BuilderService.build(numRp, numKa, rnf);
        log.info("ПР04 сформирована:\n{}", fo);

        RnPr04 record = new RnPr04();
        record.setRnf(rnf);
        record.setDsf(LocalDateTime.now());
        record.setNRp(numRp);
        record.setNKa(numKa);
        record.setFo(fo);
        rnPr04Repository.save(record);

        log.info("ПР04 сохранена в БД с rnf={}", rnf);
        return fo;
    }
}