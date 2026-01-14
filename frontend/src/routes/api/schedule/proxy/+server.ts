import type { RequestHandler } from '@sveltejs/kit';
import { json } from '@sveltejs/kit';

export const GET: RequestHandler = async ({ url, fetch }) => {
    const date = url.searchParams.get('date');
    
    if (!date) {
        return json({ error: 'Date parameter is required' }, { status: 400 });
    }
    
    try {
        const response = await fetch(`http://localhost:8081/api/schedule/operator/${date}`);
        
        if (!response.ok) {
            return json({ error: 'Failed to fetch data' }, { status: response.status });
        }
        
        const data = await response.json();
        return json(data);
        
    } catch (error) {
        console.error('Error proxying to schedule service:', error);
        return json({ error: 'Backend connection failed' }, { status: 502 });
    }
};