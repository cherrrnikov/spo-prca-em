package ru.laspace.backend.repository.vp;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.vp.Vp01Msu;

@Repository
public interface Vp01MsuRepository extends JpaRepository<Vp01Msu, Long> {
    List<Vp01Msu> findByVp01IdOrderByNumMsu(Long vp01Id);
}
