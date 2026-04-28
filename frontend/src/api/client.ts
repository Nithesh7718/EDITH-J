// Typed fetch helpers for the EDITH-J REST API

export interface ChatResponse {
  answer: string;
  intentType: string;
}

export interface StatusResponse {
  status: 'ONLINE' | 'OFFLINE';
  message: string;
}

export interface Note {
  id: string;
  title: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface Reminder {
  id: string;
  text: string;
  dueAt: string | null;
  completed: boolean;
  createdAt: string;
  updatedAt: string;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`${res.status} ${res.statusText}: ${body}`);
  }
  if (res.status === 204) return undefined as unknown as T;
  return res.json() as Promise<T>;
}

export const api = {
  // Status
  getStatus: () => request<StatusResponse>('/api/status'),

  // Chat
  sendMessage: (message: string) =>
    request<ChatResponse>('/api/chat', {
      method: 'POST',
      body: JSON.stringify({ message }),
    }),

  // Notes
  listNotes: (q?: string) =>
    request<Note[]>(`/api/notes${q ? `?q=${encodeURIComponent(q)}` : ''}`),
  createNote: (content: string) =>
    request<Note>('/api/notes', { method: 'POST', body: JSON.stringify({ content }) }),
  updateNote: (id: string, content: string) =>
    request<Note>(`/api/notes/${id}`, { method: 'PUT', body: JSON.stringify({ content }) }),
  deleteNote: (id: string) => request<void>(`/api/notes/${id}`, { method: 'DELETE' }),

  // Reminders
  listReminders: (q?: string, all?: boolean) => {
    const params = new URLSearchParams();
    if (q) params.set('q', q);
    if (all) params.set('all', 'true');
    const qs = params.toString();
    return request<Reminder[]>(`/api/reminders${qs ? `?${qs}` : ''}`);
  },
  createReminder: (text: string, dueAt: string) =>
    request<Reminder>('/api/reminders', { method: 'POST', body: JSON.stringify({ text, dueAt }) }),
  markReminderDone: (id: string) =>
    request<Reminder>(`/api/reminders/${id}/done`, { method: 'PATCH' }),
  deleteReminder: (id: string) =>
    request<void>(`/api/reminders/${id}`, { method: 'DELETE' }),
};
