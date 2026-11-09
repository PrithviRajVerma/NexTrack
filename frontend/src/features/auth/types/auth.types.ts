export interface LoginCredentials {
  email: string;
  password?: string;
}

export interface AuthTokens {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface AuthUserResponse {
  accessToken: string;
  expiresIn: number;
  user: {
    id: string;
    email: string;
    displayName: string;
    status: string;
    avatarUrl?: string | null;
  };
}
