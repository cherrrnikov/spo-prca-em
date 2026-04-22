package ru.laspace.backend.repository.programs;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.programs.ProgramsOna;

@Repository
public interface ProgramsOnaRepository extends JpaRepository<ProgramsOna, Long> {

    @Query("SELECT o FROM ProgramsOna o WHERE o.programsMain.numRp = :numRp AND o.programsMain.numKa = :numKa AND o.nOna = :nOna ORDER BY o.dN")
    List<ProgramsOna> findByNumRpAndNumKaAndNOnaOrderByDN(
            @Param("numRp") Long numRp,
            @Param("numKa") Long numKa,
            @Param("nOna") Integer nOna);
}