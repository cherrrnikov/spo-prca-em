import type { RequestHandler } from './$types';

export const GET: RequestHandler = async ({ fetch }) => {
  try {
    const response = await fetch('http://localhost:8080/api/auth/validate', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    if (response.ok) {
      return new Response(null, { status: 200 });
    } else {
      return new Response(null, { status: 401 });
    }
  } catch (error) {
    console.error('Validate error:', error);
    return new Response(null, { status: 500 });
  }
};