/** Время жизни access_token в секундах (15 минут) */
export const ACCESS_TOKEN_MAX_AGE = 15 * 60;

/** Время жизни refresh_token в секундах (7 дней) */
export const REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;

/** За сколько минут до истечения обновлять токен */
export const TOKEN_REFRESH_THRESHOLD_MINUTES = 13;

/** Интервал keepalive в миллисекундах (10 минут) */
export const KEEPALIVE_INTERVAL_MS = 10 * 60 * 1000;