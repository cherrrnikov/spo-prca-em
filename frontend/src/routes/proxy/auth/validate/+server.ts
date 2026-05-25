import { ACCESS_TOKEN_MAX_AGE, TOKEN_REFRESH_THRESHOLD_MINUTES } from '$lib/config/api.config';
import { AUTH_BASE_URL } from '$lib/config/api.server.config';
import { decodeJWT } from '$lib/utils/jwt';
import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

export const GET: RequestHandler = async ({ cookies, fetch }) => {
  const accessToken = cookies.get('access_token');
  const refreshToken = cookies.get('refresh_token');

  // Нет токенов
  if (!accessToken || !refreshToken) {
    return json({ status: 'unauthorized' }, { status: 401 });
  }

  // Проверяем, истекает ли токен
  let needsRefresh = false;
  try {
    const payload = decodeJWT(accessToken);
    if (payload?.exp) {
      const timeUntilExpiry = payload.exp - Math.floor(Date.now() / 1000);
      needsRefresh = timeUntilExpiry <= TOKEN_REFRESH_THRESHOLD_MINUTES * 60; // 2 минуты
    }
  } catch {
    needsRefresh = true;
  }

  // Если токен еще свежий, просто возвращаем успех
  if (!needsRefresh) {
    return json({ status: 'valid' });
  }

  // Токен истекает - обновляем
  try {
    const refreshResponse = await fetch(`${AUTH_BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });

    if (!refreshResponse.ok) {
      // Очищаем cookies при ошибке
      ['access_token', 'refresh_token', 'user_data'].forEach(name => {
        cookies.delete(name, { path: '/' });
      });
      return json({ status: 'unauthorized' }, { status: 401 });
    }

    const tokens = await refreshResponse.json();

    // Обновляем cookies
    cookies.set('access_token', tokens.accessToken, {
      path: '/',
      httpOnly: true,
      secure: process.env.COOKIE_SECURE === 'true',
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

    cookies.set('user_data', JSON.stringify({
      username: tokens.username || payload?.sub || '',
      firstName: tokens.firstName || '',
      lastName: tokens.lastName || '',
      roles: roles
    }), {
      path: '/',
      httpOnly: false,
      secure: process.env.COOKIE_SECURE === 'true',
      sameSite: 'strict',
      maxAge: ACCESS_TOKEN_MAX_AGE
    });

    return json({ status: 'refreshed' });

  } catch (error) {
    console.error('Keepalive error:', error);
    return json({ status: 'error' }, { status: 500 });
  }
};