package ru.laspace.backend.repository.programs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.programs.ProgramsMode;

@Repository
public interface ProgramsModeRepository extends JpaRepository<ProgramsMode, Long> {

}
