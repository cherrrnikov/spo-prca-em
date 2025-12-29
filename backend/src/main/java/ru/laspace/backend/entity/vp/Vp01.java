package ru.laspace.backend.entity.vp;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "vp01")
@Data
@RequiredArgsConstructor
public class Vp01 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "num_ka")
    private Integer numKa;

    @Column(name = "dsf")
    private LocalDateTime dsf; // дата и время составления формы ВП01

    @Column(name = "num_rp")
    private Integer numRp;

    @Column(name = "rnf")
    private Integer rnf; // регистрационный номер ФО ВП01

    @Column(name = "data_zap")
    private LocalDateTime dataZap;

    @Column(name = "form_id") // будет еще одна таблица
    private Integer formId; // пока просто int

    @Column(name = "k")
    private Integer k = 0; // кол-во записей в табл vp01_msu, относящихся к данной ВПРЦА

    @Column(name = "d")
    private Integer d = 0; // ... vp01_ona

    @Column(name = "s")
    private Integer s = 0; // ... vp01_tnp

    @Column(name = "t")
    private Integer t = 0; // ... vp01_tehns

    @Column(name = "p")
    private Integer p = 0; // ... vp01_vd

    @Column(name = "dt_n_rp")
    private LocalDateTime dtNRp; // время начала ПРЦА

    @Column(name = "dt_k_rp")
    private LocalDateTime dtKRp; // время начала ПРЦА
}
