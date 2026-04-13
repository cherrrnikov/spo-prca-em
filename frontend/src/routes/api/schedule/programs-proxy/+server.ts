import { BACKEND_BASE_URL } from "$lib/config/api.server.config";
import type { RequestHandler } from "@sveltejs/kit";
import { json } from '@sveltejs/kit';

export const POST: RequestHandler = async ({request, fetch, cookies}) => {
    const token = cookies.get('access_token');
    const body = await request.json();

    const response = await fetch(`${BACKEND_BASE_URL}/api/programs/create`, {
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