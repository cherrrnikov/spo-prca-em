package ru.laspace.backend.service.pr.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.entity.rn.RnPr01;
import ru.laspace.backend.repository.rn.RnPr01Repository;
import ru.laspace.backend.service.pr.Pr01BuilderService;
import ru.laspace.backend.service.pr.Pr01Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class Pr01ServiceImpl implements Pr01Service {

    private final Pr01BuilderService pr01BuilderService;
    private final RnPr01Repository rnPr01Repository;

    @Override
    @Transactional
    public String generateAndSave(Integer numRp, Integer numKa) {
        // Генерируем rnf = max + 1
        Integer maxRnf = rnPr01Repository.findMaxRnf();
        Integer rnf = maxRnf + 1;
        log.info("Генерация ПР01: numRp={}, numKa={}, rnf={}", numRp, numKa, rnf);

        // Строим текст ФО
        String fo = pr01BuilderService.build(numRp, numKa, rnf);
        log.info("ПР01 сформирована:\n{}", fo);

        // Сохраняем в rn_pr01
        RnPr01 record = new RnPr01();
        record.setRnf(rnf);
        record.setDsf(LocalDateTime.now());
        record.setNRp(numRp);
        record.setNKa(numKa);
        record.setFo(fo);
        rnPr01Repository.save(record);

        log.info("ПР01 сохранена в БД с rnf={}", rnf);
        return fo;
    }
}