package ru.laspace.backend.repository.vp;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.vp.Vp01Tnp;

@Repository
public interface Vp01TnpRepository extends JpaRepository<Vp01Tnp, Long> {
    List<Vp01Tnp> findByVp01IdOrderByNumTnp(Long vp01Id);
}
