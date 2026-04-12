// src/routes/api/auth/validate/+server.ts
import type { RequestHandler } from './$types';
import { json } from '@sveltejs/kit';
import { decodeJWT } from '$lib/utils/jwt';

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
      needsRefresh = timeUntilExpiry <= 2 * 60; // 2 минуты
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
    const refreshResponse = await fetch('http://localhost:8080/api/auth/refresh', {
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
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 15 * 60
    });

    if (tokens.refreshToken) {
      cookies.set('refresh_token', tokens.refreshToken, {
        path: '/',
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict',
        maxAge: 7 * 24 * 60 * 60
      });
    }

    // Обновляем user_data
    const payload = decodeJWT(tokens.accessToken);
    let roles: string[] = [];
    if (payload?.roles) {
      if (typeof payload.roles === 'string') {
        roles = payload.roles.split(',').map(r => r.trim());
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
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 15 * 60
    });

    return json({ status: 'refreshed' });

  } catch (error) {
    console.error('Keepalive error:', error);
    return json({ status: 'error' }, { status: 500 });
  }
};