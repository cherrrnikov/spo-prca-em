package ru.laspace.backend.repository.programs;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.programs.ProgramsModeKvd;

@Repository
public interface ProgramsModeKvdRepository extends JpaRepository<ProgramsModeKvd, Long> {
    Optional<ProgramsModeKvd> findByProgramsModeId(Long programsModeId);
}
