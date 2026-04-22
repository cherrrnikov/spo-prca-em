package ru.laspace.backend.repository.pr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.pr.RnPr03;

@Repository
public interface RnPr03Repository extends JpaRepository<RnPr03, Long> {
    @Query("SELECT COALESCE(MAX(r.rnf), 0) FROM RnPr03 r")
    Integer findMaxRnf();
}
