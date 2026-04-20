package ru.laspace.backend.repository.programs;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.programs.ProgramsMain;

@Repository
public interface ProgramsMainRepository extends JpaRepository<ProgramsMain, Long> {
    boolean existsByNumRpAndNumKa(Long numRp, Long numKa);

    @Query("SELECT MAX(p.numRp) FROM ProgramsMain p WHERE p.numKa = :numKa")
    Long findMaxNumRpByNumKa(@Param("numKa") Long numKa);

    Optional<ProgramsMain> findByNumRpAndNumKa(Long numRp, Long numKa);
}
