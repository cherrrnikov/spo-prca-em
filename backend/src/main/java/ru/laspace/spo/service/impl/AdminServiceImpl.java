package ru.laspace.spo.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.dto.request.CreateUserRequest;
import ru.laspace.spo.dto.request.UpdateUserRolesRequest;
import ru.laspace.spo.dto.response.UserResponse;
import ru.laspace.spo.entity.Role;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.exception.NotFoundException;
import ru.laspace.spo.repository.RoleRepository;
import ru.laspace.spo.repository.UserRepository;
import ru.laspace.spo.service.AdminService;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Создание пользователя: {} с ролями: {}",
                request.getUsername(), request.getRoles());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                    String.format("Пользователь '%s' уже существует", request.getUsername()));
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
        log.info("Пользователь создан: ID={}, username={}",
                savedUser.getId(), savedUser.getUsername());

        return mapToUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Получение списка всех пользователей");
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Получение пользователя по ID: {}", id);
        @SuppressWarnings("null")
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Пользователь с ID=%d не найден", id)));
        return mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        log.info("Получение пользователя по username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Пользователь '%s' не найден", username)));
        return mapToUserResponse(user);
    }

    @Override
    public UserResponse updateUserRoles(Long userId, UpdateUserRolesRequest request) {
        log.info("Обновление ролей пользователя ID={}, новые роли: {}",
                userId, request.getRoles());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Пользователь с ID=%d не найден", userId)));

        user.getRoles().clear();

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = fetchRolesByName(request.getRoles());
            user.getRoles().addAll(roles);
        }

        User updatedUser = userRepository.save(user);
        log.info("Роли пользователя ID={} обновлены", userId);

        return mapToUserResponse(updatedUser);
    }

    @SuppressWarnings("null")
    @Override
    public void deleteUser(Long userId) {
        log.info("Удаление пользователя ID={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Пользователь с ID=%d не найден", userId)));

        userRepository.delete(user);
        log.info("Пользователь ID={} удален", userId);
    }

    @Override
    public UserResponse disableUser(Long userId) {
        log.info("Блокировка пользователя ID={}", userId);

        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Пользователь с ID=%d не найден", userId)));

        user.setEnabled(false);
        User updatedUser = userRepository.save(user);

        return mapToUserResponse(updatedUser);
    }

    @Override
    public UserResponse enableUser(Long userId) {
        log.info("Разблокировка пользователя ID={}", userId);

        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Пользователь с ID=%d не найден", userId)));

        user.setEnabled(true);
        User updatedUser = userRepository.save(user);

        return mapToUserResponse(updatedUser);
    }

    private Set<Role> fetchRolesByName(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();

        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Роль '%s' не найдена", roleName)));
            roles.add(role);
        }

        return roles;
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .lastLogoutAt(user.getLastLogoutAt())
                .roles(user.getRoles() != null
                        ? user.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.toSet())
                        : new HashSet<>())
                .build();
    }
}