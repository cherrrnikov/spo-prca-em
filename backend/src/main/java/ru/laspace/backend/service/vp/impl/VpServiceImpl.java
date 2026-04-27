package ru.laspace.backend.service.vp.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.vp.VpCreateRequest;
import ru.laspace.backend.entity.vp.Vp01;
import ru.laspace.backend.entity.vp.Vp01Kvd;
import ru.laspace.backend.entity.vp.Vp01Msu;
import ru.laspace.backend.entity.vp.Vp01Omi;
import ru.laspace.backend.entity.vp.Vp01Ona;
import ru.laspace.backend.entity.vp.Vp01Tnp;
import ru.laspace.backend.repository.vp.Vp01KvdRepository;
import ru.laspace.backend.repository.vp.Vp01MsuRepository;
import ru.laspace.backend.repository.vp.Vp01OmiRepository;
import ru.laspace.backend.repository.vp.Vp01OnaRepository;
import ru.laspace.backend.repository.vp.Vp01Repository;
import ru.laspace.backend.repository.vp.Vp01TnpRepository;
import ru.laspace.backend.service.vp.VpService;

@Service
@Slf4j
@RequiredArgsConstructor
public class VpServiceImpl implements VpService {

    private final Vp01Repository vp01Repository;
    private final Vp01MsuRepository vp01MsuRepository;
    private final Vp01KvdRepository vp01KvdRepository;
    private final Vp01TnpRepository vp01TnpRepository;
    private final Vp01OmiRepository vp01OmiRepository;
    private final Vp01OnaRepository vp01OnaRepository;

    @Override
    @Transactional
    public Long saveVp(VpCreateRequest request) {
        log.info("=== Начало сохранения ВПРЦА ===");

        VpCreateRequest.MainData mainData = request.getMainData();

        Vp01 vp01 = new Vp01();
        vp01.setNumKa(mainData.getNumKa());
        vp01.setNumRp(mainData.getNumRp());
        vp01.setRnf(mainData.getRnf());
        vp01.setDsf(mainData.getDsf() != null ? mainData.getDsf() : LocalDateTime.now());
        vp01.setDataZap(LocalDateTime.now());
        vp01.setDtNRp(mainData.getDtNRp());
        vp01.setDtKRp(mainData.getDtKRp());

        // Счётчики — заполняем из размеров списков
        vp01.setK(request.getMsuList() != null ? request.getMsuList().size() : 0);
        vp01.setP(request.getKvdList() != null ? request.getKvdList().size() : 0);
        vp01.setS(request.getTnpList() != null ? request.getTnpList().size() : 0);
        vp01.setD(request.getOnaList() != null ? request.getOnaList().size() : 0);

        Vp01 savedVp01 = vp01Repository.save(vp01);
        log.info("Сохранена Vp01 id={}, numRp={}, numKa={}", savedVp01.getId(), savedVp01.getNumRp(),
                savedVp01.getNumKa());

        // Сохраняем каждый подынтервал съёмки отдельной записью
        if (request.getMsuList() != null) {
            int numMsu = 1;
            for (VpCreateRequest.MsuData msuData : request.getMsuList()) {
                Vp01Msu msu = new Vp01Msu();
                msu.setVp01(savedVp01);
                msu.setKodReg(msuData.getKodReg());
                msu.setNumMsu(numMsu++);
                msu.setDateNach(msuData.getDateNach());
                msu.setDateCon(msuData.getDateCon());
                msu.setComplectMsu1(msuData.getComplectMsu1());
                msu.setVd11(msuData.getVd11());
                msu.setVd12(msuData.getVd12());
                msu.setVd13(msuData.getVd13());
                msu.setIk14(msuData.getIk14());
                msu.setIk15(msuData.getIk15());
                msu.setIk16(msuData.getIk16());
                msu.setIk17(msuData.getIk17());
                msu.setIk18(msuData.getIk18());
                msu.setIk19(msuData.getIk19());
                msu.setIk110(msuData.getIk110());
                msu.setComplectMsu2(msuData.getComplectMsu2());
                msu.setVd21(msuData.getVd21());
                msu.setVd22(msuData.getVd22());
                msu.setVd23(msuData.getVd23());
                msu.setIk24(msuData.getIk24());
                msu.setIk25(msuData.getIk25());
                msu.setIk26(msuData.getIk26());
                msu.setIk27(msuData.getIk27());
                msu.setIk28(msuData.getIk28());
                msu.setIk29(msuData.getIk29());
                msu.setIk210(msuData.getIk210());
                msu.setTip(msuData.getTip());
                msu.setNumPpi(msuData.getNumPpi());
                msu.setDlit(msuData.getDlit());
                msu.setDurationCycle(msuData.getDurationCycle());
                vp01MsuRepository.save(msu);
            }
            log.info("Сохранено записей vp01_msu: {}", request.getMsuList().size());
        }

        if (request.getKvdList() != null) {
            int numKvd = 1;
            for (VpCreateRequest.KvdData kvdData : request.getKvdList()) {
                Vp01Kvd kvd = new Vp01Kvd();
                kvd.setVp01(savedVp01);
                kvd.setNumKvd(numKvd++);
                kvd.setDateNach(kvdData.getDateNach());
                kvd.setDateCon(kvdData.getDateCon());
                kvd.setComplectMsu(kvdData.getComplectMsu());
                kvd.setNumPpi(kvdData.getNumPpi());
                kvd.setDlit(kvdData.getDlit());
                vp01KvdRepository.save(kvd);
            }
            log.info("Сохранено записей vp01_kvd: {}", request.getKvdList().size());
        }

        if (request.getTnpList() != null) {
            int numTnp = 1;
            for (VpCreateRequest.TnpData tnpData : request.getTnpList()) {
                Vp01Tnp tnp = new Vp01Tnp();
                tnp.setVp01(savedVp01);
                tnp.setNumTnp(numTnp++);
                tnp.setDateNach(tnpData.getDateNach());
                tnp.setDateCon(tnpData.getDateCon());
                tnp.setNumPpi(tnpData.getNumPpi());
                tnp.setDlit(tnpData.getDlit());
                vp01TnpRepository.save(tnp);
            }
            log.info("Сохранено записей vp01_tnp: {}", request.getTnpList().size());
        }

        if (request.getOmiList() != null) {
            int numOmi = 1;
            for (VpCreateRequest.OmiData omiData : request.getOmiList()) {
                Vp01Omi omi = new Vp01Omi();
                omi.setVp01(savedVp01);
                omi.setNumOmi(numOmi++);
                omi.setTypeOmi(omiData.getTypeOmi());
                omi.setDateNach(omiData.getDateNach());
                omi.setDateCon(omiData.getDateCon());
                omi.setNumPpi(omiData.getNumPpi());
                omi.setDlit(omiData.getDlit());
                vp01OmiRepository.save(omi);
            }
            log.info("Сохранено записей vp01_omi: {}", request.getOmiList().size());
        }

        if (request.getOnaList() != null) {
            for (VpCreateRequest.OnaData onaData : request.getOnaList()) {
                Vp01Ona ona = new Vp01Ona();
                ona.setVp01(savedVp01);
                ona.setNumUstOna(onaData.getNumUstOna());
                ona.setDateNach(onaData.getDateNach());
                ona.setDateCon(onaData.getDateCon());
                ona.setNumPpi(onaData.getNumPpi());
                ona.setDlit(onaData.getDlit());
                vp01OnaRepository.save(ona);
            }
            log.info("Сохранено записей vp01_ona: {}", request.getOnaList().size());
        }

        log.info("=== Сохранение ВПРЦА завершено, id={} ===", savedVp01.getId());
        return savedVp01.getId();
    }
}