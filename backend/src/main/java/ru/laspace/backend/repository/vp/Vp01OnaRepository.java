package ru.laspace.backend.repository.vp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.vp.Vp01Ona;

@Repository
public interface Vp01OnaRepository extends JpaRepository<Vp01Ona, Long> {

}
