import type { RequestHandler } from '@sveltejs/kit';
import { json } from '@sveltejs/kit';

export const GET: RequestHandler = async ({ url, fetch, cookies }) => {
    const date = url.searchParams.get('date');
    
    if (!date) {
        return json({ error: 'Date parameter is required' }, { status: 400 });
    }
    
    const token = cookies.get('access_token');
    
    const response = await fetch(`http://localhost:8081/api/forecast/operator/${date}`, {
        headers: {
            ...(token ? {'Authorization': `Bearer ${token}`} : {})
        }
    });
    
    if (!response.ok) {
        return json({ error: 'Failed to fetch forecast data' }, { status: response.status });
    }
    
    const data = await response.json();
    return json(data);

};