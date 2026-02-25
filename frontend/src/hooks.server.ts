import type { JwtResponse } from '$lib/types/auth';
import { isTokenExpiringSoon } from '$lib/utils/jwt';
import type { Handle } from '@sveltejs/kit';
import { redirect } from '@sveltejs/kit';

export const handle: Handle = async ({ event, resolve }) => {
  const publicRoutes = ['/', '/api/auth/login', '/api/auth/refresh']; // Добавили refresh в public
  
  if (publicRoutes.includes(event.url.pathname)) {
    return await resolve(event);
  }

  const accessToken = event.cookies.get('access_token');
  const refreshToken = event.cookies.get('refresh_token');

  if (!refreshToken && event.url.pathname.startsWith('/schedule')) {
    await cleanupCookies(event.cookies);
    throw redirect(303, '/');
  }

  if (accessToken && refreshToken) {
    if (isTokenExpiringSoon(accessToken, 2)) {
      console.log('🔄 Хук: токен истекает, пробую обновить...');
      
      const refreshSuccess = await refreshAccessToken(event.cookies, refreshToken);
      
      if (!refreshSuccess) {
        console.log('❌ Хук: не удалось обновить токен, чищу куки');
        await cleanupCookies(event.cookies);
        
        // Не делаем редирект сразу, даем запросу завершиться
        // но куки уже чистые, так что следующий запрос уйдет на логин
      }
    }
  } else {
    if (event.url.pathname.startsWith('/schedule')) {
      console.log('🔒 Хук: нет токенов для защищенного маршрута');
      await cleanupCookies(event.cookies);
      throw redirect(303, '/');
    }
  }

  return await resolve(event);
};

async function refreshAccessToken(cookies: any, refreshToken: string): Promise<boolean> {
  const MAX_RETRIES = 2;
  const RETRY_DELAY = 500; // мс

  for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
    try {
      console.log(`🔄 Попытка обновления токена #${attempt}`);
      
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000); // 5 секунд таймаут

      const refreshResponse = await fetch('http://localhost:8080/api/auth/refresh', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json'
        },
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
          maxAge: 900 // 15 минут
        });

        if (newTokens.refreshToken) {
          cookies.set('refresh_token', newTokens.refreshToken, {
            path: '/',
            httpOnly: true,
            secure: process.env.NODE_ENV === 'production',
            sameSite: 'strict',
            maxAge: 604800 // 7 дней
          });
        }

        const userData = {
          username: newTokens.username,
          firstName: newTokens.firstName,
          lastName: newTokens.lastName,
          roles: newTokens.roles || []
        };

        cookies.set('user_data', JSON.stringify(userData), {
          path: '/',
          httpOnly: false,
          secure: process.env.NODE_ENV === 'production',
          sameSite: 'strict',
          maxAge: 900
        });

        console.log('✅ Хук: токен успешно обновлен');
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
      console.error(`❌ Хук: ошибка при попытке ${attempt}:`, error);
      
      if (attempt < MAX_RETRIES) {
        console.log(`Жду ${RETRY_DELAY * attempt}мс перед следующей попыткой...`);
        await new Promise(resolve => setTimeout(resolve, RETRY_DELAY * attempt));
      }
    }
  }

  console.log('❌ Хук: все попытки обновления токена исчерпаны');
  return false;
}

async function cleanupCookies(cookies: any) {
  console.log('🧹 Очистка куков');
  ['access_token', 'refresh_token', 'user_data'].forEach(name => {
    cookies.delete(name, { path: '/' });
  });
}