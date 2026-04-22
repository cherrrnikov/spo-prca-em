package ru.laspace.backend.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.entity.pr.RnPr03;
import ru.laspace.backend.repository.pr.RnPr03Repository;
import ru.laspace.backend.service.Pr03BuilderService;
import ru.laspace.backend.service.Pr03Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class Pr03ServiceImpl implements Pr03Service {

    private final Pr03BuilderService pr03BuilderService;
    private final RnPr03Repository rnPr03Repository;

    @Override
    @Transactional
    public String generateAndSave(Long numRp, Long numKa) {
        Integer maxRnf = rnPr03Repository.findMaxRnf();
        Integer rnf = maxRnf + 1;
        log.info("Генерация ПР03: numRp={}, numKa={}, rnf={}", numRp, numKa, rnf);

        String fo = pr03BuilderService.build(numRp, numKa, rnf);
        log.info("ПР03 сформирована:\n{}", fo);

        RnPr03 record = new RnPr03();
        record.setRnf(rnf);
        record.setDsf(LocalDateTime.now());
        record.setNRp(numRp.intValue());
        record.setNKa(numKa.intValue());
        record.setFo(fo);
        rnPr03Repository.save(record);

        log.info("ПР03 сохранена в БД с rnf={}", rnf);
        return fo;
    }
}