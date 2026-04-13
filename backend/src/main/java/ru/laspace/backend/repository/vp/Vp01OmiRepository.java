package ru.laspace.backend.repository.vp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.vp.Vp01Omi;

@Repository
public interface Vp01OmiRepository extends JpaRepository<Vp01Omi, Long> {

}
