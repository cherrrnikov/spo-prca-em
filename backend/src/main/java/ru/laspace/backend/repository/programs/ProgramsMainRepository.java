package ru.laspace.backend.repository.programs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.programs.ProgramsMain;

@Repository
public interface ProgramsMainRepository extends JpaRepository<ProgramsMain, Long> {
    boolean existsByNumRpAndNumKa(Integer numRp, Integer numKa);
}
