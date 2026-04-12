import type { RequestHandler } from './$types';
import { json } from '@sveltejs/kit';
import { decodeJWT } from '$lib/utils/jwt';
import type { JwtResponse } from '$lib/types/auth';

export const GET: RequestHandler = async ({ cookies, fetch }) => {
    const refreshToken = cookies.get('refresh_token');

    if (!refreshToken) {
        return json({ status: 'unauthorized' }, { status: 401 });
    }

    try {
        const refreshResponse = await fetch('http://localhost:8080/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });

        if (!refreshResponse.ok) {
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