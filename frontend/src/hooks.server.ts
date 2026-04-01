import type { JwtResponse } from '$lib/types/auth';
import { decodeJWT, isTokenExpiringSoon } from '$lib/utils/jwt';
import type { Handle } from '@sveltejs/kit';
import { redirect } from '@sveltejs/kit';

let refreshPromise: Promise<boolean> | null = null;

export const handle: Handle = async ({ event, resolve }) => {
  const publicRoutes = ['/', '/api/auth/login', '/api/auth/refresh'];
  
  if (publicRoutes.includes(event.url.pathname)) {
    return await resolve(event);
  }

  const accessToken = event.cookies.get('access_token');
  const refreshToken = event.cookies.get('refresh_token');

  // Если нет refresh_token, отправляем на логин
  if (!refreshToken && event.url.pathname.startsWith('/schedule')) {
    await cleanupCookies(event.cookies);
    throw redirect(303, '/');
  }

  // Восстанавливаем user_data, если она пропала, но токен валидный
  if (accessToken && !event.cookies.get('user_data')) {
    try {
      const payload = decodeJWT(accessToken);
      if (payload && payload.exp * 1000 > Date.now()) {
        let roles: string[] = [];
        if (payload.roles) {
          if (typeof payload.roles === 'string') {
            roles = payload.roles.split(',').map(r => r.trim());
          } else if (Array.isArray(payload.roles)) {
            roles = payload.roles;
          }
        }
        
        const userData = {
          username: payload.sub,
          firstName: '',
          lastName: '',
          roles: roles
        };
        
        event.cookies.set('user_data', JSON.stringify(userData), {
          path: '/',
          httpOnly: false,
          secure: process.env.NODE_ENV === 'production',
          sameSite: 'strict',
          maxAge: 900
        });
        
        console.log('🔄 Восстановлена user_data из токена');
      }
    } catch (e) {
      console.log('Ошибка восстановления user_data:', e);
    }
  }

  // Проверяем, нужно ли обновлять access_token
  if (accessToken && refreshToken && isTokenExpiringSoon(accessToken, 2)) {
    console.log(`🔄 Хук: токен истекает для ${event.url.pathname}, пробую обновить...`);
    
    if (refreshPromise) {
      console.log('⏳ Обновление уже выполняется, ждем...');
      const success = await refreshPromise;
      if (!success) {
        await cleanupCookies(event.cookies);
        if (event.url.pathname.startsWith('/schedule')) {
          throw redirect(303, '/');
        }
      }
    } else {
      refreshPromise = refreshAccessToken(event.cookies, refreshToken);
      
      try {
        const success = await refreshPromise;
        if (!success) {
          await cleanupCookies(event.cookies);
          if (event.url.pathname.startsWith('/schedule')) {
            throw redirect(303, '/');
          }
        }
      } finally {
        refreshPromise = null;
      }
    }
  }

  return await resolve(event);
};

async function refreshAccessToken(cookies: any, refreshToken: string): Promise<boolean> {
  const MAX_RETRIES = 2;
  const RETRY_DELAY = 500;

  for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
    try {
      console.log(`🔄 Попытка обновления токена #${attempt}`);
      
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);

      const refreshResponse = await fetch('http://localhost:8080/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
        signal: controller.signal
      });

      clearTimeout(timeoutId);

      if (refreshResponse.ok) {
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
        
        const userData = {
          username: newTokens.username || payload?.sub || '',
          firstName: newTokens.firstName || '',
          lastName: newTokens.lastName || '',
          roles: roles
        };

        cookies.set('user_data', JSON.stringify(userData), {
          path: '/',
          httpOnly: false,
          secure: process.env.NODE_ENV === 'production',
          sameSite: 'strict',
          maxAge: 900
        });

        console.log('✅ Хук: токен и user_data успешно обновлены');
        return true;
      }

      if (refreshResponse.status === 401) {
        console.log('❌ Хук: refresh token недействителен (401)');
        return false;
      }

      console.log(`⚠️ Хук: ошибка ${refreshResponse.status}, попытка ${attempt}/${MAX_RETRIES}`);
      
      if (attempt < MAX_RETRIES) {
        await new Promise(resolve => setTimeout(resolve, RETRY_DELAY * attempt));
      }

    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        console.log(`⏱️ Таймаут при попытке ${attempt}`);
      } else {
        console.error(`❌ Хук: ошибка при попытке ${attempt}:`, error);
      }
      
      if (attempt < MAX_RETRIES) {
        await new Promise(resolve => setTimeout(resolve, RETRY_DELAY * attempt));
      }
    }
  }

  console.log('❌ Хук: все попытки обновления токена исчерпаны');
  return false;
}

async function cleanupCookies(cookies: any) {
  ['access_token', 'refresh_token', 'user_data'].forEach(name => {
    cookies.delete(name, { path: '/' });
  });
}