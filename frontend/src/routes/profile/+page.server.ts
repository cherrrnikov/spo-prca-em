import { redirect } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ cookies, fetch }) => {
  const accessToken = cookies.get('access_token');

  if (!accessToken) {
    throw redirect(303, '/');
  }
  
  try {
    const userData = cookies.get('user_data');
    
    if (userData) {
      return {
        user: JSON.parse(userData)
      };
    }
    
    throw redirect(303, '/');
    
  } catch (error) {
    console.error('Ошибка загрузки профиля:', error);
    throw redirect(303, '/');
  }
};