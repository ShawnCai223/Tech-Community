import client from './client';
import type { ApiResponse, MessageSummary, PageResponse } from '../types/api';

export async function getLetters(page = 0, limit = 10) {
  const res = await client.get<ApiResponse<PageResponse<any>>>('/messages/letters', { params: { page, limit } });
  return res.data.data;
}

export async function getLetterDetail(conversationId: string, page = 0, limit = 20) {
  const res = await client.get<ApiResponse<PageResponse<any>>>(`/messages/letters/${conversationId}`, { params: { page, limit } });
  return res.data.data;
}

export async function sendLetter(toName: string, content: string) {
  const res = await client.post<ApiResponse<void>>('/messages/letters', { toName, content });
  return res.data;
}

export async function getNotices() {
  const res = await client.get<ApiResponse<any>>('/messages/notices');
  return res.data.data;
}

export async function getNoticeDetail(topic: string, page = 0, limit = 10, entityType?: number) {
  const res = await client.get<ApiResponse<PageResponse<any>>>(`/messages/notices/${topic}`, { params: { page, limit, entityType } });
  return res.data.data;
}

export async function markNoticeRead(id: number) {
  const res = await client.put<ApiResponse<void>>(`/messages/notices/${id}/read`);
  return res.data;
}

export async function getMessageSummary() {
  const res = await client.get<ApiResponse<MessageSummary>>('/messages/summary');
  return res.data.data;
}
