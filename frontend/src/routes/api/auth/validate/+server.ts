import type { JwtResponse } from '$lib/types/auth';
import { decodeJWT, isTokenExpiringSoon } from '$lib/utils/jwt';
import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

export const GET: RequestHandler = async ({ cookies, fetch }) => {
    const accessToken = cookies.get('access_token');
    const refreshToken = cookies.get('refresh_token');

    // Нет refresh токена — сессия мертва
    if (!refreshToken) {
        return json({ status: 'unauthorized' }, { status: 401 });
    }

    // Токен ещё живой и не скоро истекает — всё ок
    if (accessToken && !isTokenExpiringSoon(accessToken, 3)) {
        return json({ status: 'ok' });
    }

    // Токен истекает скоро или уже истёк — обновляем
    try {
        const refreshResponse = await fetch('http://localhost:8080/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });

        if (!refreshResponse.ok) {
            // Refresh токен невалиден — чистим всё
            ['access_token', 'refresh_token', 'user_data'].forEach(name => {
                cookies.delete(name, { path: '/' });
            });
            return json({ status: 'unauthorized' }, { status: 401 });
        }

        const newTokens: JwtResponse = await refreshResponse.json();

        cookies.set('access_token', newTokens.accessToken, {
            path: '/',
            httpOnly: true,
            secure: process.env.NODE_ENV === 'production',
            sameSite: 'strict',
            maxAge: 900
        });

        if (newTokens.refreshToken) {
            cookies.set('refresh_token', newTokens.refreshToken, {
                path: '/',
                httpOnly: true,
                secure: process.env.NODE_ENV === 'production',
                sameSite: 'strict',
                maxAge: 604800
            });
        }

        const payload = decodeJWT(newTokens.accessToken);
        let roles: string[] = [];
        if (payload?.roles) {
            if (typeof payload.roles === 'string') {
                roles = payload.roles.split(',').map(r => r.trim());
            } else if (Array.isArray(payload.roles)) {
                roles = payload.roles;
            }
        }

        cookies.set('user_data', JSON.stringify({
            username: newTokens.username || payload?.sub || '',
            firstName: newTokens.firstName || '',
            lastName: newTokens.lastName || '',
            roles: roles
        }), {
            path: '/',
            httpOnly: false,
            secure: process.env.NODE_ENV === 'production',
            sameSite: 'strict',
            maxAge: 900
        });

        return json({ status: 'refreshed' });

    } catch (error) {
        console.error('Keepalive refresh error:', error);
        return json({ status: 'error' }, { status: 500 });
    }
};