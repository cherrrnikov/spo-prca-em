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
    lastLoginAt: string;
}

export interface UserCacheDto {
    id: number;
    username: string;
    firstName: string;
    lastName: string;
    lastLoginAt: string;
    lastLogoutAt: string;
    enabled: boolean;
    failedAttempts: number;
    accountLocked: boolean;
    lockTime: string;
    lastFailedLogin: string;
    roles: string[];
}

export interface UserResponse {
    username: string;
    firstName: string;
    lastName: string;
    enabled: boolean;
    accountLocked: boolean;
    failedAttempts: number;
    lastLoginAt: string;
    lastLogoutAt: string;
    roles: string[];
}