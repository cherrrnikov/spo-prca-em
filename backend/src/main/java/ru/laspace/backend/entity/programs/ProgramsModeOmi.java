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
@Table(name = "programs_mode_omi")
@Data
@RequiredArgsConstructor
public class ProgramsModeOmi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_programs_mode")
    private ProgramsMode programsMode;

    @Column(name = "num_omi")
    private Integer numOmi; // сквозной номер режима

    @Column(name = "type_omi")
    private Integer typeOmi;

    @Column(name = "date_nach")
    private LocalDateTime dateNach;

    @Column(name = "date_con")
    private LocalDateTime dateCon;

    @Column(name = "dlit")
    private Integer dlit;

}
