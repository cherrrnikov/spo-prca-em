import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

export const GET: RequestHandler = async ({ cookies, fetch }) => {
  // Пустой ответ, но сам факт вызова запустит handle хук
  // где проверятся и обновятся токены
  return json({ status: 'ok', timestamp: new Date().toISOString() });
};