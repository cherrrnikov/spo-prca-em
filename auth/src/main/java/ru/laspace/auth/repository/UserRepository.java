package ru.laspace.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.laspace.auth.entity.Role;
import ru.laspace.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameInternal(@Param("username") String username);

    @Query("SELECT r FROM Role r WHERE r.name IN :roleNames")
    Set<Role> findRolesByNames(@Param("roleNames") Set<String> roleNames);

    boolean existsByUsername(String username);

    @Override
    @CacheEvict(value = "users", key = "#user.username")
    <S extends User> S save(S user);

    @Modifying
    @Query("UPDATE User u SET u.lastLogoutAt = :timestamp WHERE u.id = :userId")
    void updateLastLogout(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);
}