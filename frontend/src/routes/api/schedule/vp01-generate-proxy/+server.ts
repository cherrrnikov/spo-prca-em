import { BACKEND_BASE_URL } from "$lib/config/api.server.config";
import type { RequestHandler } from "@sveltejs/kit";

export const POST: RequestHandler = async ({ request, fetch, cookies }) => {
    const token = cookies.get('access_token');
    const { numRp, numKa } = await request.json();

    const response = await fetch(
        `${BACKEND_BASE_URL}/api/vp/${numRp}/${numKa}/vp01/generate`,
        {
            method: 'POST',
            headers: {
                ...(token ? { 'Authorization': `Bearer ${token}` } : {})
            }
        }
    );

    const text = await response.text();
    return new Response(text, { status: response.status });
};