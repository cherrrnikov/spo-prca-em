export interface LoginRequest {
    username: string;
    password: string;
}

export interface RefreshTokenRequest {
    refreshToken: string;
}

export interface JwtResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    username: string;
    firstName: string;
    lastName: string;
    roles: string[];
}

export interface UserResponse {
    username: string;
    firstName: string;
    lastName: string;
    enabled: boolean;
    accountLocked: boolean;
    failedAttempts: number;
    roles: string[];
}

export interface CachedUser {
    id: number;
    username: string;
    firstName: string;
    lastName: string;
    lastLoginAt: string | null;
    lastLogoutAt: string | null;
    enabled: boolean;
    failedAttempts: number;
    accountLocked: boolean;
    lockTime: string | null;
    roles: string[];
}

