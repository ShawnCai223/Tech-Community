import client from './client';
import type { ApiResponse } from '../types/api';

export async function addComment(postId: number, entityType: number, entityId: number, content: string, targetId = 0) {
  const res = await client.post<ApiResponse<void>>(`/posts/${postId}/comments`, {
    entityType,
    entityId,
    content,
    targetId,
  });
  return res.data;
}
