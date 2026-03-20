import client from './client';
import type { ApiResponse } from '../types/api';

export async function toggleLike(entityType: number, entityId: number, entityUserId: number, postId: number) {
  const res = await client.post<ApiResponse<{ likeCount: number; likeStatus: number }>>('/likes', {
    entityType, entityId, entityUserId, postId,
  });
  return res.data.data;
}
