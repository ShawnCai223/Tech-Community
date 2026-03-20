import client from './client';
import type { ApiResponse, PageResponse } from '../types/api';

export async function searchPosts(keyword: string, page = 0, limit = 10) {
  const res = await client.get<ApiResponse<PageResponse<any>>>('/search', { params: { keyword, page, limit } });
  return res.data.data;
}
