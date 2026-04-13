import { BACKEND_BASE_URL } from '$lib/config/api.server.config';
import type { RequestHandler } from '@sveltejs/kit';
import { json } from '@sveltejs/kit';

export const GET: RequestHandler = async ({ url, fetch, cookies }) => {
    const date = url.searchParams.get('date');
    
    if (!date) {
        return json({ error: 'Date parameter is required' }, { status: 400 });
    }
    
    const token = cookies.get('access_token');

    const response = await fetch(`${BACKEND_BASE_URL}/api/bort/${date}`, {
        headers: {
            ...(token ? {'Authorization': `Bearer ${token}`} : {})
        }
    });
    
    if (!response.ok) {
        if (response.status === 404) {
            return json({ error: 'Bort data not found' }, { status: 404 });
        }
        return json({ error: 'Failed to fetch bort data' }, { status: response.status });
    }
    
    const data = await response.json();
    return json(data);
};