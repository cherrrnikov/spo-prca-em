package ru.laspace.auth.service.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.auth.dto.request.CreateUserRequest;
import ru.laspace.auth.dto.response.UserResponse;
import ru.laspace.auth.entity.Role;
import ru.laspace.auth.entity.User;
import ru.laspace.auth.exception.NotFoundException;
import ru.laspace.auth.repository.RoleRepository;
import ru.laspace.auth.repository.UserRepository;
import ru.laspace.auth.security.CustomUserDetails;
import ru.laspace.auth.service.LoginAttemptService;
import ru.laspace.auth.service.RefreshTokenService;
import ru.laspace.auth.service.UserCacheService;
import ru.laspace.auth.service.UserService;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final UserCacheService userCacheService;

    // Публичные методы

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        log.info("Getting current user: {}", userDetails.getUsername());

        User user = userCacheService.findByUsername(userDetails.getUsername())
                .map(cachedUser -> userCacheService.toEntityWithRoles(cachedUser))
                .orElseThrow(() -> new NotFoundException("User " + userDetails.getUsername()));

        return buildUserResponse(user);
    }

    // Админские методы

    @Override
    @PreAuthorize("hasRole('ADMIN')")
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
        userCacheService.updateCachedUser(savedUser);

        log.info("User created with ID: {}", savedUser.getId());
        return buildUserResponse(savedUser);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Cacheable(value = "allUsers", unless = "#result.isEmpty()")
    public List<UserResponse> getAllUsers() {
        log.info("Getting all users");
        return userRepository.findAll().stream()
                .map(this::buildUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Getting user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id));
        return buildUserResponse(user);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        log.info("Getting user by username: {}", username);
        User user = userCacheService.findByUsername(username)
                .map(cachedUser -> userCacheService.toEntityWithRoles(cachedUser))
                .orElseThrow(() -> new NotFoundException("User " + username));
        return buildUserResponse(user);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "allUsers", allEntries = true)
    public UserResponse updateUserRoles(Long userId, Set<String> roleNames) {
        log.info("Updating roles for user ID: {} with roles: {}", userId, roleNames);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId));

        user.getRoles().clear();

        if (roleNames != null && !roleNames.isEmpty()) {
            Set<Role> roles = fetchRolesByName(roleNames);
            user.getRoles().addAll(roles);
        }

        User updatedUser = userRepository.save(user);
        userCacheService.updateCachedUser(updatedUser);

        // Отзываем все токены при изменении ролей
        refreshTokenService.revokeAllUserTokens(userId);

        log.info("Roles updated for user: {}", updatedUser.getUsername());
        return buildUserResponse(updatedUser);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "allUsers", allEntries = true)
    public void deleteUser(Long userId) {
        log.info("Deleting user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId));

        refreshTokenService.revokeAllUserTokens(userId);
        userCacheService.evictUserFromCache(user.getUsername());
        userRepository.delete(user);

        log.info("User deleted: {}", user.getUsername());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "allUsers", allEntries = true)
    public UserResponse disableUser(Long userId) {
        log.info("Disabling user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId));

        user.setEnabled(false);
        refreshTokenService.revokeAllUserTokens(userId);

        User updatedUser = userRepository.save(user);
        userCacheService.updateCachedUser(updatedUser);

        return buildUserResponse(updatedUser);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "allUsers", allEntries = true)
    public UserResponse enableUser(Long userId) {
        log.info("Enabling user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId));

        user.setEnabled(true);

        User updatedUser = userRepository.save(user);
        userCacheService.updateCachedUser(updatedUser);

        return buildUserResponse(updatedUser);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "allUsers", allEntries = true)
    public UserResponse unlockUserAccount(Long userId) {
        log.info("Unlocking account for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId));

        loginAttemptService.unlockAccount(user.getUsername());

        User updatedUser = userRepository.save(user);
        userCacheService.updateCachedUser(updatedUser);

        return buildUserResponse(updatedUser);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "allUsers", allEntries = true)
    public UserResponse resetUserPassword(Long userId, String newPassword) {
        log.info("Resetting password for user ID: {}", userId);

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.resetFailedAttempts();

        refreshTokenService.revokeAllUserTokens(userId);

        User updatedUser = userRepository.save(user);
        userCacheService.updateCachedUser(updatedUser);

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