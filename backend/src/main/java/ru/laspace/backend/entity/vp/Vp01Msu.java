package ru.laspace.backend.entity.vp;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "vp01_msu")
@Data
@RequiredArgsConstructor
public class Vp01Msu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pr")
    private Vp01 vp01;

    @Column(name = "kod_reg")
    private Integer kodReg;

    @Column(name = "num_msu")
    private Integer numMsu;

    @Column(name = "date_nach")
    private LocalDateTime dateNach;

    @Column(name = "date_con")
    private LocalDateTime dateCon;

    @Column(name = "complect_msu1")
    private Integer complectMsu1;

    @Column(name = "vd11")
    private Integer vd11;

    @Column(name = "vd12")
    private Integer vd12;

    @Column(name = "vd13")
    private Integer vd13;

    @Column(name = "ik14")
    private Integer ik14;

    @Column(name = "ik15")
    private Integer ik15;

    @Column(name = "ik16")
    private Integer ik16;

    @Column(name = "ik17")
    private Integer ik17;

    @Column(name = "ik18")
    private Integer ik18;

    @Column(name = "ik19")
    private Integer ik19;

    @Column(name = "ik110")
    private Integer ik110;

    @Column(name = "complect_msu2")
    private Integer complectMsu2;

    @Column(name = "vd21")
    private Integer vd21;

    @Column(name = "vd22")
    private Integer vd22;

    @Column(name = "vd23")
    private Integer vd23;

    @Column(name = "ik24")
    private Integer ik24;

    @Column(name = "ik25")
    private Integer ik25;

    @Column(name = "ik26")
    private Integer ik26;

    @Column(name = "ik27")
    private Integer ik27;

    @Column(name = "ik28")
    private Integer ik28;

    @Column(name = "ik29")
    private Integer ik29;

    @Column(name = "ik210")
    private Integer ik210;

    @Column(name = "tip")
    private Integer tip; // тип съемки: 1 - штатная, 2 - учащенная

    @Column(name = "num_ppi")
    private Integer numPpi;

    @Column(name = "dlit")
    private Integer dlit;

    @Column(name = "duration_cycle")
    private Integer durationCycle;
}
