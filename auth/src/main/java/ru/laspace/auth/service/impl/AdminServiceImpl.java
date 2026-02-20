package ru.laspace.auth.service.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.dto.request.CreateUserRequest;
import ru.laspace.auth.dto.request.UpdateUserRolesRequest;
import ru.laspace.auth.dto.response.UserResponse;
import ru.laspace.auth.entity.Role;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.exception.NotFoundException;
import ru.laspace.auth.repository.RoleRepository;
import ru.laspace.auth.repository.UserRepository;
import ru.laspace.auth.service.AdminService;
import ru.laspace.auth.service.LoginAttemptService;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    @Override
    @CacheEvict(value = "allUsers", allEntries = true)
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user: {} with roles: {}", request.getUsername(), request.getRoles());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("User already exists: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(request.isEnabled());

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = fetchRolesByName(request.getRoles());
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);
        log.info("User created with ID: {}", savedUser.getId());

        return buildUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Getting all users");
        return userRepository.findAll().stream()
                .map(this::buildUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Getting user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User" + id));
        return buildUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        log.info("Getting user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User" + username));
        return buildUserResponse(user);
    }

    @Override
    @CacheEvict(value = "allUsers", allEntries = true)
    public UserResponse updateUserRoles(Long userId, UpdateUserRolesRequest request) {
        log.info("Updating roles for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User" + userId));

        user.getRoles().clear();

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = fetchRolesByName(request.getRoles());
            user.getRoles().addAll(roles);
        }

        User updatedUser = userRepository.save(user);
        log.info("Roles updated for user: {}", updatedUser.getUsername());

        return buildUserResponse(updatedUser);
    }

    @Override
    @CacheEvict(value = "allUsers", allEntries = true)
    public void deleteUser(Long userId) {
        log.info("Deleting user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User" + userId));
        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());
    }

    @Override
    @CacheEvict(value = "allUsers", allEntries = true)
    public UserResponse disableUser(Long userId) {
        log.info("Disabling user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User" + userId));
        user.setEnabled(false);
        User updatedUser = userRepository.save(user);
        return buildUserResponse(updatedUser);
    }

    @Override
    @CacheEvict(value = "allUsers", allEntries = true)
    public UserResponse enableUser(Long userId) {
        log.info("Enabling user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User" + userId));
        user.setEnabled(true);
        User updatedUser = userRepository.save(user);
        return buildUserResponse(updatedUser);
    }

    @Override
    @CacheEvict(value = "allUsers", allEntries = true)
    public UserResponse unlockUserAccount(Long userId) {
        log.info("Unlocking account for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User" + userId));
        loginAttemptService.unlockAccount(user.getUsername());
        return buildUserResponse(user);
    }

    @Override
    @CacheEvict(value = "allUsers", allEntries = true)
    public UserResponse resetUserPassword(Long userId, String newPassword) {
        log.info("Resetting password for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User" + userId));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.resetFailedAttempts();
        User updatedUser = userRepository.save(user);
        return buildUserResponse(updatedUser);
    }

    private Set<Role> fetchRolesByName(Set<String> roleNames) {
        return roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName)))
                .collect(Collectors.toSet());
    }

    private UserResponse buildUserResponse(User user) {
        return UserResponse.builder()
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .accountLocked(user.isAccountLocked())
                .failedAttempts(user.getFailedAttempts())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }
}