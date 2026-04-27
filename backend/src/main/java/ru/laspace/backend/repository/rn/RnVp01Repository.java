package ru.laspace.backend.repository.rn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.rn.RnVp01;

@Repository
public interface RnVp01Repository extends JpaRepository<RnVp01, Long> {
    @Query("SELECT COALESCE(MAX(r.rnf), 0) FROM RnVp01 r")
    Integer findMaxRnf();
}
