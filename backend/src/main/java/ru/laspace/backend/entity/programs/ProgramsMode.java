package ru.laspace.backend.entity.programs;

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
@Table(name = "programs_mode")
@Data
@RequiredArgsConstructor
public class ProgramsMode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_programs_main")
    private ProgramsMain programsMain;

    @Column(name = "num_rp")
    private Integer numRp;

    @Column(name = "num_ka")
    private Integer numKa;

    @Column(name = "date_on")
    private LocalDateTime dateOn;

    @Column(name = "date_off")
    private LocalDateTime dateOff;

    @Column(name = "kod_mode")
    private Integer kodMode;

    @Column(name = "num_ppi")
    private Integer numPpi;

    @Column(name = "dlit")
    private Integer dlit;

    @Column(name = "zakazchik")
    private String zakazchik;
}
