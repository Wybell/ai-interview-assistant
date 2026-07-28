import { defineStore } from 'pinia';

import { login, register } from '@/api/auth-api';
import type { AuthCredentials, AuthSession } from '@/types/auth';
import { clearSession, readSession, writeSession } from '@/utils/session';

const initialSession = readSession();

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: initialSession?.token ?? '',
    username: initialSession?.username ?? '',
    role: initialSession?.role ?? 'USER',
    isSubmitting: false,
  }),
  actions: {
    applySession(session: AuthSession) {
      this.token = session.token;
      this.username = session.username;
      this.role = session.role ?? 'USER';
      writeSession(session);
    },
    async login(credentials: AuthCredentials) {
      this.isSubmitting = true;
      try {
        const session = await login(credentials);
        this.applySession(session);
      } finally {
        this.isSubmitting = false;
      }
    },
    async register(credentials: AuthCredentials) {
      this.isSubmitting = true;
      try {
        const session = await register(credentials);
        this.applySession(session);
      } finally {
        this.isSubmitting = false;
      }
    },
    logout() {
      this.token = '';
      this.username = '';
      this.role = 'USER';
      clearSession();
    },
  },
});
