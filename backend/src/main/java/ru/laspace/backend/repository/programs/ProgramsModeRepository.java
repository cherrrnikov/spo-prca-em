package ru.laspace.backend.repository.programs;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.programs.ProgramsMode;

@Repository
public interface ProgramsModeRepository extends JpaRepository<ProgramsMode, Long> {
    @Query("SELECT m FROM ProgramsMode m WHERE m.programsMain.numRp = :numRp AND m.programsMain.numKa = :numKa ORDER BY m.dateOn ASC")
    List<ProgramsMode> findByNumRpAndNumKaOrderByDateOn(@Param("numRp") Integer numRp, @Param("numKa") Integer numKa);
}
