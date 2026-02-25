import { redirect } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ cookies }) => {
  const accessToken = cookies.get('access_token');

  if (!accessToken) {
    throw redirect(303, '/');
  }
  
  try {
    const userDataStr = cookies.get('user_data');
    
    if (userDataStr) {
      const userData = JSON.parse(userDataStr);
      return { user: userData };
    }
    
    throw redirect(303, '/');
    
  } catch (error) {
    console.error('Ошибка загрузки профиля:', error);
    throw redirect(303, '/');
  }
};