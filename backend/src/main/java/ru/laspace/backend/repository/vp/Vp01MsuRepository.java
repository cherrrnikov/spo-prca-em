package ru.laspace.backend.repository.vp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.vp.Vp01Msu;

@Repository
public interface Vp01MsuRepository extends JpaRepository<Vp01Msu, Long> {
}
