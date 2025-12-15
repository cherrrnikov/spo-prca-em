package ru.laspace.auth.service;

public interface LoginAttemptService {
    void loginSucceeded(String username);

    void loginFailed(String username);

    boolean isAccountLocked(String username);

    int getRemainingAttempts(String username);

    void unlockAccount(String username);

    void applyLoginDelay();
}