package ru.laspace.backend.repository.rn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.rn.RnPr01;

@Repository
public interface RnPr01Repository extends JpaRepository<RnPr01, Long> {
    @Query("SELECT COALESCE(MAX(r.rnf), 0) FROM RnPr01 r")
    Integer findMaxRnf();
}
