package ru.laspace.backend.repository.vp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.vp.Vp01;

@Repository
public interface Vp01Repository extends JpaRepository<Vp01, Long> {
    boolean existsByNumRpAndNumKa(Long numRp, Long numKa);
}
