package ru.laspace.backend.repository.pr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.pr.RnPr04;

@Repository
public interface RnPr04Repository extends JpaRepository<RnPr04, Long> {
    @Query("SELECT COALESCE(MAX(r.rnf), 0) FROM RnPr04 r")
    Integer findMaxRnf();
}