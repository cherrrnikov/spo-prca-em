import { isTokenExpiringSoon } from '$lib/utils/jwt';
import type { Handle } from '@sveltejs/kit';
import { redirect } from '@sveltejs/kit';

export const handle: Handle = async ({ event, resolve }) => {
  const publicRoutes = ['/', '/api/auth/login', '/api/auth/refresh'];
  if (publicRoutes.includes(event.url.pathname)) {
    return await resolve(event);
  }

  const accessToken = event.cookies.get('access_token');
  const refreshToken = event.cookies.get('refresh_token');

  if (!accessToken && event.url.pathname.startsWith('/profile')) {
    throw redirect(303, '/');
  }

  if (accessToken && refreshToken) {
    const isExpiringOrExpired = isTokenExpiringSoon(accessToken, 0); 
    
    if (isExpiringOrExpired) {
      try {
        console.log('Access token требует обновления, пытаюсь обновить...');
        
        const refreshResponse = await fetch('http://localhost:8080/api/auth/refresh', {
          method: 'POST',
          headers: { 
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`
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
          
          event.cookies.set('refresh_token', newTokens.refreshToken, {
            path: '/',
            httpOnly: true,
            secure: process.env.NODE_ENV === 'production',
            sameSite: 'strict',
            maxAge: 604800
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

          console.log('✅ Токены успешно обновлены для:', newTokens.username);
        } else {
          console.log('Refresh token невалиден, выполняю logout');
          await cleanupCookies(event.cookies);
          throw redirect(303, '/');
        }
      } catch (error) {
        console.error('Ошибка при обновлении токена:', error);
        await cleanupCookies(event.cookies);
        throw redirect(303, '/');
      }
    }
  }

  if (accessToken && !refreshToken && event.url.pathname.startsWith('/profile')) {

    console.log('Есть access token, но нет refresh token');
  }

  return await resolve(event);
};

async function cleanupCookies(cookies: any) {
  ['access_token', 'refresh_token', 'user_data'].forEach(name => {
    cookies.delete(name, { path: '/' });
  });
}