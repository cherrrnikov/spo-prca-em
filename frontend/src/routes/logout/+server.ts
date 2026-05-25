import { AUTH_BASE_URL } from '$lib/config/api.server.config.js';
import type { RequestHandler } from '@sveltejs/kit';
import { redirect } from '@sveltejs/kit';

export const POST: RequestHandler = async ({ cookies, fetch }) => {
    const accessToken = cookies.get('access_token');
    const refreshToken = cookies.get('refresh_token');

    try {
        if (refreshToken) {
            await fetch(`${AUTH_BASE_URL}/api/auth/logout`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify({ refreshToken })
            });
        }
    } catch { /* ignore */ }

    ['access_token', 'refresh_token', 'user_data'].forEach(name => {
        cookies.delete(name, { 
            path: '/',
            sameSite: 'strict',
            secure: process.env.COOKIE_SECURE === 'true'
        });
    });

    throw redirect(303, '/login');
};