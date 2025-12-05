package ru.laspace.spo.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.spo.entity.User;
import ru.laspace.spo.repository.UserRepository;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            log.warn("Пользователь не найден: {}", username);
            return new UsernameNotFoundException("Пользователь не найден");
        });

        if (!user.isEnabled()) {
            log.warn("Аккаунт отключен, ID: {}", user.getId());
            throw new UsernameNotFoundException("Аккаунт отключен.");
        }

        return new UserDetailsImpl(user);
    }

}
