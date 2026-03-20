import client from './client';
import type { ApiResponse, AuthTokens } from '../types/api';

export async function getCaptcha() {
  const res = await client.get<ApiResponse<{ captchaOwner: string; captchaImage: string }>>('/auth/captcha');
  return res.data.data;
}

export async function login(params: {
  username: string;
  password: string;
  captchaCode: string;
  captchaOwner: string;
  rememberMe?: boolean;
}) {
  const res = await client.post<ApiResponse<AuthTokens>>('/auth/login', params);
  return res.data;
}

export async function register(params: {
  username: string;
  password: string;
  email: string;
}) {
  const res = await client.post<ApiResponse<void>>('/auth/register', params);
  return res.data;
}

export async function logout(refreshToken: string) {
  const res = await client.post<ApiResponse<void>>('/auth/logout', { refreshToken });
  return res.data;
}
