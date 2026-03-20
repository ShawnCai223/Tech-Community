import client from './client';
import type { ApiResponse, PageResponse } from '../types/api';

export async function getPosts(page = 0, limit = 10, orderMode = 0) {
  const res = await client.get<ApiResponse<PageResponse<Record<string, unknown>>>>('/posts', {
    params: { page, limit, orderMode },
  });
  return res.data.data;
}

export async function getPostDetail(id: number, commentPage = 0, commentLimit = 5) {
  const res = await client.get<ApiResponse<Record<string, unknown>>>(`/posts/${id}`, {
    params: { commentPage, commentLimit },
  });
  return res.data.data;
}

export async function createPost(title: string, content: string) {
  const res = await client.post<ApiResponse<{ id: number }>>('/posts', { title, content });
  return res.data;
}
