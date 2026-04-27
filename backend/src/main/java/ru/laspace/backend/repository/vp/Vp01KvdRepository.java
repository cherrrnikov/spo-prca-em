package ru.laspace.backend.repository.vp;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.vp.Vp01Kvd;

@Repository
public interface Vp01KvdRepository extends JpaRepository<Vp01Kvd, Long> {
    List<Vp01Kvd> findByVp01IdOrderByNumKvd(Long vp01Id);
}
