import client from './client';
import type { ApiResponse } from '../types/api';

export async function uploadFile(file: File): Promise<{ url: string; type: 'image' | 'video' }> {
  const formData = new FormData();
  formData.append('file', file);
  // Let axios auto-set Content-Type with the correct multipart boundary.
  // The default 'application/json' from the client must be cleared.
  const res = await client.post<ApiResponse<{ url: string; type: string }>>('/upload', formData, {
    headers: { 'Content-Type': undefined },
  });
  return res.data.data as { url: string; type: 'image' | 'video' };
}
