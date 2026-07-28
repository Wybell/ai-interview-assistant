import type { AuthSession } from '@/types/auth';

const SESSION_KEY = 'ai-interview-assistant.session';

function hasSessionStorage(): boolean {
  return typeof window !== 'undefined' && typeof window.sessionStorage !== 'undefined';
}

export function readSession(): AuthSession | null {
  if (!hasSessionStorage()) {
    return null;
  }

  const raw = window.sessionStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    const session = JSON.parse(raw) as Partial<AuthSession>;
    if (typeof session.token === 'string' && typeof session.username === 'string') {
      return { token: session.token, username: session.username, role: session.role === 'ADMIN' ? 'ADMIN' : 'USER' };
    }
  } catch {
    window.sessionStorage.removeItem(SESSION_KEY);
  }

  return null;
}

export function writeSession(session: AuthSession): void {
  if (hasSessionStorage()) {
    window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }
}

export function clearSession(): void {
  if (hasSessionStorage()) {
    window.sessionStorage.removeItem(SESSION_KEY);
  }
}
