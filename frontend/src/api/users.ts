import client from './client';
import type { ApiResponse, UserProfile } from '../types/api';

export async function getMe() {
  const res = await client.get<ApiResponse<UserProfile>>('/users/me');
  return res.data.data;
}

export async function getUserProfile(id: number) {
  const res = await client.get<ApiResponse<UserProfile>>(`/users/${id}`);
  return res.data.data;
}

export async function updateMyPassword(params: {
  originalPassword: string;
  newPassword: string;
  confirmPassword: string;
}) {
  const res = await client.post<ApiResponse<void>>('/users/me/password', params);
  return res.data;
}
