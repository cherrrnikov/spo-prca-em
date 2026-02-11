import type { RequestHandler } from '@sveltejs/kit';
import { error, json } from '@sveltejs/kit';

export const GET: RequestHandler = async ({ url, fetch }) => {
    const date = url.searchParams.get('date');
    
    if (!date) {
        return json({ error: 'Date parameter is required' }, { status: 400 });
    }
    
    try {
        const response = await fetch(`http://localhost:8081/api/vki/correction/${date}`);
        
        if (!response.ok) {
            if (response.status === 404) {
                return json({ main: null, impulses: [], totalImpulses: 0 });
            }
            throw error(response.status, 'Failed to fetch VKI data');
        }
        
        const data = await response.json();
        return json(data);
        
    } catch (err) {
        console.error('Error proxying to VKI service:', err);
        throw error(502, 'Backend connection failed');
    }
};