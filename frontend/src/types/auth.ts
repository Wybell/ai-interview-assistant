export interface AuthCredentials {
  username: string;
  password: string;
}

export interface AuthSession {
  token: string;
  username: string;
  role?: 'USER' | 'ADMIN';
}
