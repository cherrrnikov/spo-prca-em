package ru.laspace.spo.service;

import ru.laspace.spo.entity.User;

public interface AuthService {
    User authenticate(String username, String rawPassword);

    User findById(Long userId);
    boolean existsByUsername(String username);
}
