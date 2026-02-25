package ru.laspace.auth.config;

import java.time.LocalDateTime;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private int maxFailedAttempts = 5;
    private int accountLockDurationMinutes = 15;
    private boolean enableBruteForceProtection = true;
    private int loginDelayMillis = 1000;

    public boolean isLockExpired(LocalDateTime lockTime) {
        if (lockTime == null)
            return true;
        return LocalDateTime.now().isAfter(
                lockTime.plusMinutes(accountLockDurationMinutes));
    }
}
