package ru.laspace.auth.service;

import java.util.List;

import ru.laspace.auth.dto.response.UserResponse;

public interface UserService {
    UserResponse getCurrentUser();

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse updateUserRoles(Long id, List<String> roles);

    void deleteUser(Long id);

    UserResponse enableUser(Long id);

    UserResponse disableUser(Long id);

    UserResponse unlockAccount(Long id);

    UserResponse resetPassword(Long id, String newPassword);
}
