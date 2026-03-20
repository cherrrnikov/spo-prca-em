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
@Table(name = "programs_ona")
@Data
@RequiredArgsConstructor
public class ProgramsOna {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_programs")
    private ProgramsMain programsMain;

    @Column(name = "n_ona")
    private Integer nOna; // 1-ОНА1, 2-ОНА2

    @Column(name = "d_n")
    private LocalDateTime dN;

    @Column(name = "d_k")
    private LocalDateTime dK;

    @Column(name = "n_ppi")
    private Integer nPpi;

    @Column(name = "type_mode")
    private Integer typeMode; // тип режима, с которого начался интервал работы ОНА
}