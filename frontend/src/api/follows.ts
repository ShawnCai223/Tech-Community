import client from './client';
import type { ApiResponse } from '../types/api';

export async function follow(entityType: number, entityId: number) {
  const res = await client.post<ApiResponse<void>>('/follows', { entityType, entityId });
  return res.data;
}

export async function unfollow(entityType: number, entityId: number) {
  const res = await client.delete<ApiResponse<void>>('/follows', { data: { entityType, entityId } });
  return res.data;
}

export async function getFollowees(userId: number, offset = 0, limit = 10) {
  const res = await client.get<ApiResponse<any[]>>(`/follows/followees/${userId}`, { params: { offset, limit } });
  return res.data.data;
}

export async function getFollowers(userId: number, offset = 0, limit = 10) {
  const res = await client.get<ApiResponse<any[]>>(`/follows/followers/${userId}`, { params: { offset, limit } });
  return res.data.data;
}
