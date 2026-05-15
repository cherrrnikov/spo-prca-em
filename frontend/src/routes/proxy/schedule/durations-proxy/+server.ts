import { BACKEND_BASE_URL } from "$lib/config/api.server.config";
import type { RequestHandler } from "@sveltejs/kit";
import { json } from '@sveltejs/kit';

export const GET: RequestHandler = async ({fetch, cookies}) => {
    const token = cookies.get('access_token');

    const response = await fetch(`${BACKEND_BASE_URL}/api/schedule/mode-durations`, {
        headers: {
            ...(token ? {'Authorization': `Bearer ${token}`} : {})
        }
    });

    if (!response.ok) {
        return json({}, {status: response.status});
    }

    const data = await response.json();
    return json(data);
}