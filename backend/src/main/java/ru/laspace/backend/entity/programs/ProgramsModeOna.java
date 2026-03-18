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
@Table(name = "programs_mode_ona")
@Data
@RequiredArgsConstructor
public class ProgramsModeOna {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_programs_mode")
    private ProgramsMode programsMode;

    @Column(name = "d_n")
    private LocalDateTime dN;

    @Column(name = "d_k")
    private LocalDateTime dK;

    @Column(name = "n_ona")
    private Integer nOna;

    @Column(name = "n_ppi")
    private Integer nPpi;
}
