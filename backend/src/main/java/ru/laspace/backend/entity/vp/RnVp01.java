package ru.laspace.backend.entity.vp;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "rn_vp01", uniqueConstraints = {
        @UniqueConstraint(name = "rn_vp01_unique", columnNames = { "rnf" })
})
@Data
@RequiredArgsConstructor
public class RnVp01 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rnf", nullable = false)
    private Integer rnf;

    @Column(name = "dsf")
    private LocalDateTime dsf;

    @Column(name = "n_rp")
    private Integer nRp;

    @Column(name = "n_ka")
    private Integer nKa;

    @Column(name = "fo", columnDefinition = "text")
    private String fo;
}
