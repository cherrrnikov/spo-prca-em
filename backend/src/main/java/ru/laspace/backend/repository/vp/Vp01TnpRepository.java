package ru.laspace.backend.repository.vp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.laspace.backend.entity.vp.Vp01Tnp;

@Repository
public interface Vp01TnpRepository extends JpaRepository<Vp01Tnp, Long> {

}
