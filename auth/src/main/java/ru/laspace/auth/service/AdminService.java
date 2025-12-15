package ru.laspace.auth.service;

import java.util.List;

import ru.laspace.auth.dto.request.CreateUserRequest;
import ru.laspace.auth.dto.request.UpdateUserRolesRequest;
import ru.laspace.auth.dto.response.UserResponse;

public interface AdminService {
    UserResponse createUser(CreateUserRequest request);

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long userId);

    UserResponse getUserByUsername(String username);

    UserResponse updateUserRoles(Long userId, UpdateUserRolesRequest request);

    void deleteUser(Long userId);

    UserResponse disableUser(Long userId);

    UserResponse enableUser(Long userId);

    UserResponse unlockUserAccount(Long userId);

    UserResponse resetUserPassword(Long userId, String newPassword);
}
