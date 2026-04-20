package ru.laspace.backend.repository.programs;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.programs.ProgramsModeOna;

@Repository
public interface ProgramsModeOnaRepository extends JpaRepository<ProgramsModeOna, Long> {
    Optional<ProgramsModeOna> findByProgramsModeId(Long programsModeId);
}
