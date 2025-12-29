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
@Table(name = "programs_mode_kvd")
@Data
@RequiredArgsConstructor
public class ProgramsModeKvd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_programs_mode")
    private ProgramsMode programsMode;

    @Column(name = "pr_msu")
    private Integer prMsu;

    @Column(name = "pr_bssd")
    private Integer prBssd;

    @Column(name = "pr_zg")
    private Integer prZg;
}
