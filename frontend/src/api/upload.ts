import client from './client';
import type { ApiResponse } from '../types/api';

export async function uploadFile(file: File): Promise<{ url: string; type: 'image' | 'video' }> {
  const formData = new FormData();
  formData.append('file', file);
  const res = await client.post<ApiResponse<{ url: string; type: string }>>('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return res.data.data as { url: string; type: 'image' | 'video' };
}
