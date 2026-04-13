import { env } from '$env/dynamic/private';

/** Адрес сервиса аутентификации (серверная сторона) */
export const AUTH_BASE_URL = env.AUTH_INTERNAL_URL || 'http://localhost:8080';

/** Адрес сервиса данных планирования (серверная сторона) */
export const BACKEND_BASE_URL = env.BACKEND_INTERNAL_URL || 'http://localhost:8081';