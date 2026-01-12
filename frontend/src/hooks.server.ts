import { isTokenExpiringSoon } from '$lib/utils/jwt';
import type { Handle } from '@sveltejs/kit';
import { redirect } from '@sveltejs/kit';

export const handle: Handle = async ({ event, resolve }) => {
  const publicRoutes = ['/', '/api/auth/login'];
  
  if (publicRoutes.includes(event.url.pathname)) {
    return await resolve(event);
  }

  const accessToken = event.cookies.get('access_token');
  const refreshToken = event.cookies.get('refresh_token');

  if (!refreshToken && event.url.pathname.startsWith('/schedule')) {
    await cleanupCookies(event.cookies);
    throw redirect(303, '/');
  }

  if (accessToken && refreshToken && isTokenExpiringSoon(accessToken, 1)) {
    try {
      console.log('Хук: токен истекает, обновляю автоматически...');
      
      const refreshResponse = await fetch('http://localhost:8080/api/auth/refresh', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ refreshToken })
      });

      if (refreshResponse.ok) {
        const newTokens = await refreshResponse.json();
        
        event.cookies.set('access_token', newTokens.accessToken, {
          path: '/',
          httpOnly: true,
          secure: process.env.NODE_ENV === 'production',
          sameSite: 'strict',
          maxAge: 900
        });
        
        const userData = {
          username: newTokens.username,
          firstName: newTokens.firstName,
          lastName: newTokens.lastName,
          roles: newTokens.roles || [],
          lastLoginAt: newTokens.lastLoginAt
        };

        event.cookies.set('user_data', JSON.stringify(userData), {
          path: '/',
          httpOnly: false,
          secure: process.env.NODE_ENV === 'production',
          sameSite: 'strict',
          maxAge: 900
        });

        console.log('✅ Хук: токен обновлен');
      }
    } catch (error) {
      console.error('Хук: ошибка обновления токена:', error);
    }
  }

  return await resolve(event);
};

async function cleanupCookies(cookies: any) {
  ['access_token', 'refresh_token', 'user_data'].forEach(name => {
    cookies.delete(name, { path: '/' });
  });
}