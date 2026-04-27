package ru.laspace.backend.entity.programs;

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
@Table(name = "programs_main", uniqueConstraints = {
        @UniqueConstraint(name = "programs_main_unique", columnNames = { "num_rp", "num_ka" })
})
@Data
@RequiredArgsConstructor
public class ProgramsMain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "num_rp", nullable = false)
    private Integer numRp; // Начинается с 1 а не с 0

    @Column(name = "num_ka", nullable = false)
    private Integer numKa;

    @Column(name = "date_on")
    private LocalDateTime dateOn;

    @Column(name = "date_off")
    private LocalDateTime dateOff;

    @Column(name = "type_rp")
    private Integer typeRp; // 3-основная, !5-корректирующая

    @Column(name = "pr_otpr")
    private Integer prOtpr; // 0-не отправлена, 1-отправлена, -1 отменена; ...
}
