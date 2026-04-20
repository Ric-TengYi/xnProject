import http from './request';

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface MessageRecord {
  id: string;
  title?: string | null;
  content?: string | null;
  category?: string | null;
  channel?: string | null;
  status?: string | null;
  statusLabel?: string | null;
  priority?: string | null;
  priorityLabel?: string | null;
  linkUrl?: string | null;
  bizType?: string | null;
  bizId?: string | null;
  senderName?: string | null;
  receiverType?: string | null;
  sendTime?: string | null;
  readTime?: string | null;
}

export interface MessageSummary {
  total: number;
  unread: number;
  read: number;
}

export interface MessageQueryParams {
  keyword?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
  pageNo?: number;
  pageSize?: number;
}

const mapRecord = (item: any): MessageRecord => ({
  id: String(item.id || ''),
  title: item.title || null,
  content: item.content || null,
  category: item.category || null,
  channel: item.channel || null,
  status: item.status || null,
  statusLabel: item.statusLabel || null,
  priority: item.priority || null,
  priorityLabel: item.priorityLabel || null,
  linkUrl: item.linkUrl || null,
  bizType: item.bizType || null,
  bizId: item.bizId || null,
  senderName: item.senderName || null,
  receiverType: item.receiverType || null,
  sendTime: item.sendTime || null,
  readTime: item.readTime || null,
});

export async function fetchMessages(params: MessageQueryParams = {}) {
  const res = await http.get<PageResult<MessageRecord>>('/messages', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapRecord),
  };
}

export async function fetchMessageSummary() {
  const res = await http.get<MessageSummary>('/messages/summary');
  return res.data;
}

export async function markMessageRead(id: string) {
  const res = await http.put<MessageRecord>(`/messages/${id}/read`);
  return mapRecord(res.data || {});
}

export async function markAllMessagesRead() {
  const res = await http.put<{ updated: number }>('/messages/read-all');
  return Number(res.data?.updated || 0);
}

export async function exportMessages(params: MessageQueryParams = {}) {
  const res = await http.get<Blob>('/messages/export', {
    params,
    responseType: 'blob',
  });
  return res.data;
}
