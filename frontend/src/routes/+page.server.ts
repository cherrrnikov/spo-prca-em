import { redirect } from '@sveltejs/kit';

export async function load({ cookies }) {
    const accessToken = cookies.get('access_token');

    if (accessToken) {
        throw redirect(303, '/schedule');
    }

    throw redirect(303, '/login');
}