// src/hooks.server.ts
import { ACCESS_TOKEN_MAX_AGE } from '$lib/config/api.config';
import { AUTH_BASE_URL } from '$lib/config/api.server.config';
import type { Handle } from '@sveltejs/kit';
import { redirect } from '@sveltejs/kit';

interface JwtPayload {
  sub?: string;
  exp?: number;
  roles?: string | string[];
  username?: string;
  firstName?: string;
  lastName?: string;
}

// Функция для декодирования JWT (без валидации)
function decodeJWT(token: string): JwtPayload | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = parts[1];
    return JSON.parse(Buffer.from(payload, 'base64').toString());
  } catch {
    return null;
  }
}

// Проверка, истекает ли токен в ближайшие N минут
function isTokenExpiringSoon(token: string, minutesBeforeExpiry: number = 2): boolean {
  try {
    const payload = decodeJWT(token);
    if (!payload?.exp) return true;
    
    const currentTime = Math.floor(Date.now() / 1000);
    const timeUntilExpiry = payload.exp - currentTime;
    const threshold = minutesBeforeExpiry * 60;
    
    return timeUntilExpiry <= threshold;
  } catch {
    return true;
  }
}

// Проверка, истек ли токен
function isTokenExpired(token: string): boolean {
  try {
    const payload = decodeJWT(token);
    if (!payload?.exp) return true;
    
    const currentTime = Math.floor(Date.now() / 1000);
    return payload.exp <= currentTime;
  } catch {
    return true;
  }
}

// Глобальный promise для предотвращения множественных обновлений
let refreshPromise: Promise<boolean> | null = null;

// Функция обновления токенов
async function refreshTokens(cookies: any, refreshToken: string): Promise<boolean> {
  try {
    const response = await fetch(`${AUTH_BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      console.error('❌ Refresh failed:', response.status);
      return false;
    }

    const tokens = await response.json();
    
    // Обновляем cookies
    cookies.set('access_token', tokens.accessToken, {
      path: '/',
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: ACCESS_TOKEN_MAX_AGE
    });

    // Обновляем user_data
    const payload = decodeJWT(tokens.accessToken);
    let roles: string[] = [];
    if (payload?.roles) {
      if (typeof payload.roles === 'string') {
        roles = payload.roles.split(',').map(r => r.trim().replace('ROLE_', ''));
        roles.sort((a, b) => {
          if (a === 'ADMIN') return -1;
          if (b === 'ADMIN') return 1;
          return 0;
        });
      } else if (Array.isArray(payload.roles)) {
        roles = payload.roles;
      }
    }

    const userData = {
      username: tokens.username || payload?.sub || '',
      firstName: tokens.firstName || '',
      lastName: tokens.lastName || '',
      roles: roles
    };

    cookies.set('user_data', JSON.stringify(userData), {
      path: '/',
      httpOnly: false,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: ACCESS_TOKEN_MAX_AGE
    });

    console.log('✅ Tokens refreshed successfully');
    return true;

  } catch (error) {
    console.error('❌ Refresh error:', error);
    return false;
  }
}

// Очистка cookies
async function clearAuthCookies(cookies: any) {
  ['access_token', 'refresh_token', 'user_data'].forEach(name => {
    cookies.delete(name, { path: '/' });
  });
}

// Главный хук
export const handle: Handle = async ({ event, resolve }) => {
  const { cookies, url } = event;
  
  // Публичные маршруты - пропускаем
  const publicRoutes = ['/', '/api/auth/login', '/api/auth/refresh'];
  if (publicRoutes.includes(url.pathname)) {
    return await resolve(event);
  }

  const accessToken = cookies.get('access_token');
  const refreshToken = cookies.get('refresh_token');

  // Нет refresh_token - отправляем на логин
  if (!refreshToken) {
    if (url.pathname.startsWith('/schedule')) {
      await clearAuthCookies(cookies);
      throw redirect(303, '/');
    }
    return await resolve(event);
  }

  // Нет access_token, но есть refresh_token - пробуем обновить
  if (!accessToken && refreshToken) {
    console.log('🔄 No access token, refreshing...');
    const success = await refreshTokens(cookies, refreshToken);
    if (!success) {
      await clearAuthCookies(cookies);
      throw redirect(303, '/');
    }
    return await resolve(event);
  }

  // Есть оба токена - проверяем нужно ли обновление
  if (accessToken && refreshToken) {
    const needsRefresh = isTokenExpiringSoon(accessToken, 2) || isTokenExpired(accessToken);
    
    if (needsRefresh) {
      console.log('🔄 Token expiring soon, refreshing...');
      
      // Используем глобальный promise для предотвращения дублирования
      if (!refreshPromise) {
        refreshPromise = refreshTokens(cookies, refreshToken);
      }
      
      const success = await refreshPromise;
      refreshPromise = null;
      
      if (!success) {
        await clearAuthCookies(cookies);
        throw redirect(303, '/login');
      }
    }
  }

  // Восстанавливаем user_data если она пропала
  if (accessToken && !cookies.get('user_data')) {
    const payload = decodeJWT(accessToken);
    if (payload && !isTokenExpired(accessToken)) {
      let roles: string[] = [];
      if (payload?.roles) {
        if (typeof payload.roles === 'string') {
          roles = payload.roles.split(',').map(r => r.trim());
        } else if (Array.isArray(payload.roles)) {
          roles = payload.roles;
        }
      }
      
      const userData = {
        username: payload.sub || '',
        firstName: '',
        lastName: '',
        roles: roles
      };
      
      cookies.set('user_data', JSON.stringify(userData), {
        path: '/',
        httpOnly: false,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict',
        maxAge: ACCESS_TOKEN_MAX_AGE
      });
      
      console.log('🔄 Restored user_data from token');
    }
  }

  return await resolve(event);
};