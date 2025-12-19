export interface JwtPayload {
  sub: string;     
  exp: number;        
  iat: number;      
  iss?: string;        
  roles?: string;
}

export function decodeJWT(token: string): JwtPayload | null {
  try {
    if (!token || token.split('.').length !== 3) {
      return null;
    }
    
    const payloadBase64 = token.split('.')[1];
    if (!payloadBase64) return null;
    
    const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('Ошибка декодирования JWT:', error);
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = decodeJWT(token);
  if (!payload || !payload.exp) return true;
  
  const expiresAt = payload.exp * 1000; 
  return Date.now() >= expiresAt;
}

export function isTokenExpiringSoon(token: string, thresholdMinutes = 2): boolean {
  const payload = decodeJWT(token);
  if (!payload || !payload.exp) return true;
  
  const expiresAt = payload.exp * 1000;
  const timeLeft = expiresAt - Date.now();
  
  if (timeLeft <= 0) return true;
  
  const thresholdMs = thresholdMinutes * 60 * 1000;
  return timeLeft < thresholdMs;
}

export function getTokenExpiry(token: string): Date | null {
  const payload = decodeJWT(token);
  if (!payload?.exp) return null;
  return new Date(payload.exp * 1000);
}

export function getTokenUsername(token: string): string | null {
  const payload = decodeJWT(token);
  return payload?.sub || null;
}