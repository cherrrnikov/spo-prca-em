package ru.laspace.backend.repository.programs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.programs.ProgramsModeOmi;

@Repository
public interface ProgramsModeOmiRepository extends JpaRepository<ProgramsModeOmi, Long> {

}
