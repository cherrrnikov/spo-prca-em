import { ACCESS_TOKEN_MAX_AGE, REFRESH_TOKEN_MAX_AGE } from "$lib/config/api.config.js";
import { AUTH_BASE_URL } from "$lib/config/api.server.config.js";
import type { JwtResponse, LoginRequest } from "$lib/types/auth";
import { fail, redirect, type Actions } from "@sveltejs/kit";

class RedirectError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'RedirectError';
  }
}

export const actions = {
    login: async ({ request, cookies, fetch }) => {
        const formData = await request.formData();
        const username = formData.get('username') as string;
        const password = formData.get('password') as string;
        const userAgent = request.headers.get('user-agent') || 'unknown';

        if (!username?.trim() || !password) {
            return fail(400, {
                error: 'Имя пользователя и пароль обязательны',
                username: username || ''
            });
        }

        const loginRequest: LoginRequest = {
            username: username.trim(),
            password
        };

        try {
            
            const response = await fetch(`${AUTH_BASE_URL}/api/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    'User-Agent': userAgent
                },
                body: JSON.stringify(loginRequest)
            });
            
            if (!response.ok) {
                let errorMessage = getSpringErrorMessage(response.status);
                try {
                    const text = await response.text();
                    if (text) {
                        try {
                            const errorData = JSON.parse(text);
                            errorMessage = errorData.message || errorData.error || errorMessage;
                        } catch {
                            errorMessage = text;
                        }
                    }
                } catch { /* ignore */ }

                return fail(response.status, { username, error: errorMessage });
            }

            const data: JwtResponse = await response.json();
            
            cookies.set('access_token', data.accessToken, {
                path: '/',
                httpOnly: true,
                secure: process.env.COOKIE_SECURE === 'true',
                sameSite: 'strict',
                maxAge: ACCESS_TOKEN_MAX_AGE 
            });
            
            cookies.set('refresh_token', data.refreshToken, {
                path: '/',
                httpOnly: true,
                secure: process.env.COOKIE_SECURE === 'true',
                sameSite: 'strict',
                maxAge: REFRESH_TOKEN_MAX_AGE
            });

            const userData = {
                username: data.username,
                firstName: data.firstName,
                lastName: data.lastName,
                roles: (data.roles || []).sort((a: string, b: string) => {
                    if (a === 'ADMIN') return -1;
                    if (b === 'ADMIN') return 1;
                    return 0;
                })
            };

            cookies.set('user_data', JSON.stringify(userData), {
                path: '/',
                httpOnly: false, 
                secure: process.env.COOKIE_SECURE === 'true',
                sameSite: 'strict',
                maxAge: ACCESS_TOKEN_MAX_AGE
            });
            throw new RedirectError('redirect:/schedule');
            
        } catch (error) {
            console.error('Поймано исключение в login action:', error);
            
            if (error instanceof RedirectError) {
                throw redirect(303, '/schedule');
            }
            
            if (error instanceof Error && 
                (error.message.includes('redirect') || error.message.includes('RedirectError'))) {
                throw error;
            }

            console.error("Ошибка при логине: ", error);
            return fail(500, {
                username,
                error: 'Сервис авторизации недоступен'
            });
        }
    },

    logout: async ({ cookies, fetch }) => {
        const accessToken = cookies.get('access_token');
        const refreshToken = cookies.get('refresh_token');

        try {
            if (refreshToken) {
                
                const response = await fetch(`${AUTH_BASE_URL}/api/auth/logout`, {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${accessToken}`
                    },
                    body: JSON.stringify({ refreshToken })
                });

                if (!response.ok) {
                } else {
                }
            }
        } catch (error) {
            console.error('Ошибка при выходе:', error);
        }

        // Очищаем все куки независимо от результата
        ['access_token', 'refresh_token', 'user_data'].forEach(name => {
            cookies.delete(name, { path: '/' });
        });
        throw redirect(303, '/');
    }
} satisfies Actions;

function getSpringErrorMessage(status: number): string {
    const messages: Record<number, string> = {
        400: 'Неверный формат запроса',
        401: 'Неверный логин или пароль',
        403: 'Доступ запрещен. Аккаунт может быть заблокирован',
        429: 'Слишком много попыток входа',
        500: 'Внутренняя ошибка сервера'
    };

    return messages[status] || `Ошибка ${status}`;
}

export async function load({ cookies }) {
    const accessToken = cookies.get('access_token');

    if (accessToken) {
        throw redirect(303, '/schedule');
    }

    return {};
}