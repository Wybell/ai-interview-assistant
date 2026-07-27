import { request } from '@/api/client';
import type { AuthCredentials, AuthSession } from '@/types/auth';

function toFormData(credentials: AuthCredentials): URLSearchParams {
  return new URLSearchParams({
    username: credentials.username,
    password: credentials.password,
  });
}

export function login(credentials: AuthCredentials): Promise<AuthSession> {
  return request<AuthSession>({
    url: '/auth/login',
    method: 'post',
    data: toFormData(credentials),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  });
}

export function register(credentials: AuthCredentials): Promise<AuthSession> {
  return request<AuthSession>({
    url: '/auth/register',
    method: 'post',
    data: toFormData(credentials),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  });
}
