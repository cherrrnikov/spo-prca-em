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
@Table(name = "vp01_kvd")
@Data
@RequiredArgsConstructor
public class Vp01Kvd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pr")
    private Vp01 vp01;

    @Column(name = "num_kvd")
    private Integer numKvd; // сквозной номер режима

    @Column(name = "date_nach")
    private LocalDateTime dateNach;

    @Column(name = "date_con")
    private LocalDateTime dateCon;

    @Column(name = "complect_msu")
    private Integer complectMsu; // признак задействования МСУ1, 0 - МСУ1, 1 - МСУ2

    @Column(name = "num_ppi")
    private Integer numPpi;

    @Column(name = "dlit")
    private Integer dlit;
}
