package ru.laspace.backend.entity.programs;

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
@Table(name = "programs_mode_msu")
@Data
@RequiredArgsConstructor
public class ProgramsModeMsu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_programs_mode")
    private ProgramsMode programsMode;

    @Column(name = "tip")
    private Integer tip; // тип съемки: 1-штатная, 2-учащенная

    @Column(name = "reg")
    private Integer reg; // режим съемки: 0-ДС, 1-НС, 10-СР1, 11-СР2, 100-СР3

    @Column(name = "dlit")
    private Integer dlit;

    @Column(name = "pr_msu1")
    private Integer prMsu1; // признак задействования МСУ1, 0 - не задействован, 1 - задействован

    @Column(name = "pr_vd_msu1")
    private Integer prVdMsu1; // признак проведения съемки в ВД МСУ1, 0 - нет, 1 - да

    @Column(name = "pr_ik_msu1")
    private Integer prIkMsu1; // признак проведения съемки в ИК МСУ1, 0 - нет, 1 - да

    @Column(name = "vd1_msu1")
    private Integer vd1Msu1; // признак задействования ВД1 на МСУ1: 0 - не задействовано, 1 - задействован

    @Column(name = "vd2_msu1")
    private Integer vd2Msu1;

    @Column(name = "vd3_msu1")
    private Integer vd3Msu1;

    @Column(name = "ik4_msu1")
    private Integer ik4Msu1; // признак задействования ИК4 на МСУ1: 0 - не задействован, 1 - задействован

    @Column(name = "ik5_msu1")
    private Integer ik5Msu1;

    @Column(name = "ik6_msu1")
    private Integer ik6Msu1;

    @Column(name = "ik7_msu1")
    private Integer ik7Msu1;

    @Column(name = "ik8_msu1")
    private Integer ik8Msu1;

    @Column(name = "ik9_msu1")
    private Integer ik9Msu1;

    @Column(name = "ik10_msu1")
    private Integer ik10Msu1;

    @Column(name = "pr_msu2")
    private Integer prMsu2;

    @Column(name = "pr_vd_msu2")
    private Integer prVdMsu2;

    @Column(name = "pr_ik_msu2")
    private Integer prIkMsu2;

    @Column(name = "vd1_msu2")
    private Integer vd1Msu2;

    @Column(name = "vd2_msu2")
    private Integer vd2Msu2;

    @Column(name = "vd3_msu2")
    private Integer vd3Msu2;

    @Column(name = "ik4_msu2")
    private Integer ik4Msu2;

    @Column(name = "ik5_msu2")
    private Integer ik5Msu2;

    @Column(name = "ik6_msu2")
    private Integer ik6Msu2;

    @Column(name = "ik7_msu2")
    private Integer ik7Msu2;

    @Column(name = "ik8_msu2")
    private Integer ik8Msu2;

    @Column(name = "ik9_msu2")
    private Integer ik9Msu2;

    @Column(name = "ik10_msu2")
    private Integer ik10Msu2;

    @Column(name = "pr_bssd")
    private Integer prBssd; // признак вкл БССД

    @Column(name = "pr_zg")
    private Integer prZg; // признак вкл ЗГ

    @Column(name = "pr_otkl_zg_bssd")
    private Integer prOtklZgBssd;

}
