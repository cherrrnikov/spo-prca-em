package ru.laspace.backend.repository.pr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ru.laspace.backend.entity.pr.RnPr01;

public interface RnPr01Repository extends JpaRepository<RnPr01, Long> {
    @Query("SELECT COALESCE(MAX(r.rnf), 0) FROM RnPr01 r")
    Integer findMaxRnf();
}
