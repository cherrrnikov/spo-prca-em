package ru.laspace.auth.service;

import java.util.List;
import java.util.Set;

import ru.laspace.auth.dto.request.CreateUserRequest;
import ru.laspace.auth.dto.response.UserResponse;

public interface UserService {
    // Публичные методы
    UserResponse getCurrentUser();

    // Админские методы
    UserResponse createUser(CreateUserRequest request);

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse getUserByUsername(String username);

    UserResponse updateUserRoles(Long userId, Set<String> roleNames);

    void deleteUser(Long userId);

    UserResponse disableUser(Long userId);

    UserResponse enableUser(Long userId);

    UserResponse unlockUserAccount(Long userId);

    UserResponse resetUserPassword(Long userId, String newPassword);
}