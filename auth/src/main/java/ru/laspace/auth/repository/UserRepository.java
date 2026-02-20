package ru.laspace.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.laspace.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Cacheable(value = "users", key = "#username", unless = "#result == null")
    Optional<User> findByUsername(String username);

    @CacheEvict(value = "users", key = "#user.username")
    @Override
    <S extends User> S save(S user);

    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.lastLogoutAt = :timestamp WHERE u.id = :userId")
    void updateLastLogout(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);
}
