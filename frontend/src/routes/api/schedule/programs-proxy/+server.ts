import type { RequestHandler } from "@sveltejs/kit";
import { json } from '@sveltejs/kit';

export const POST: RequestHandler = async ({request, fetch, cookies}) => {
    const token = cookies.get('access_token');
    const body = await request.json();

    const response = await fetch('http://localhost:8081/api/programs/create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...(token ? {'Authorization': `Bearer ${token}`} : {})
        },
        body: JSON.stringify(body)
    });

    if (!response.ok) {
        return json({error: 'Failed to save program'}, {status: response.status});
    }

    const text = await response.text();
    return json(text ? JSON.parse(text) : null);
}