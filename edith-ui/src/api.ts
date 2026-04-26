const API = 'http://localhost:8080/api';

export interface ChatMessage { role: string; content: string; timestamp: string; }
export interface Note { id: string; title: string; content: string; createdAt: string; updatedAt: string; }
export interface Reminder { id: string; text: string; dueAt: string; completed: boolean; createdAt: string; }
export interface Settings { autoSendVoiceInput: boolean; preferShortcutApps: boolean; allowWebFallback: boolean; whatsappAppFirst: boolean; devSmokeLaunchersEnabled: boolean; }
export interface Telemetry { clarificationPrompts: number; worldCircuitOpenHits: number; localKbEmptyHits: number; }

async function req<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) throw new Error((await res.json().catch(() => ({error: res.statusText}))).error ?? res.statusText);
  if (res.status === 204) return undefined as T;
  return res.json();
}

// Chat
export const sendChat = (message: string) =>
  req<ChatMessage>('/chat', { method: 'POST', body: JSON.stringify({ message }) });

// Notes
export const getNotes = (q?: string) =>
  req<Note[]>('/notes' + (q ? `?q=${encodeURIComponent(q)}` : ''));
export const createNote = (content: string) =>
  req<Note>('/notes', { method: 'POST', body: JSON.stringify({ content }) });
export const updateNote = (id: string, content: string) =>
  req<Note>(`/notes/${id}`, { method: 'PUT', body: JSON.stringify({ content }) });
export const deleteNote = (id: string) =>
  req<void>(`/notes/${id}`, { method: 'DELETE' });

// Reminders
export const getReminders = () => req<Reminder[]>('/reminders');
export const createReminder = (text: string, dueHint: string) =>
  req<Reminder>('/reminders', { method: 'POST', body: JSON.stringify({ text, dueHint }) });
export const completeReminder = (id: string) =>
  req<{ success: boolean }>(`/reminders/${id}/complete`, { method: 'POST' });
export const deleteReminder = (id: string) =>
  req<void>(`/reminders/${id}`, { method: 'DELETE' });

// Clipboard
export const getClipboard = () => req<{ text: string }>('/clipboard');
export const writeClipboard = (text: string) =>
  req<{ success: boolean }>('/clipboard', { method: 'POST', body: JSON.stringify({ text }) });

// Files
export const getRecentFiles = () => req<string[]>('/files/recent');

// Telemetry
export const getTelemetry = () => req<Telemetry>('/telemetry');
export const resetTelemetry = () => req<{ success: boolean }>('/telemetry/reset', { method: 'POST' });

// Settings
export const getSettings = () => req<Settings>('/settings');
export const updateSettings = (patch: Partial<Settings>) =>
  req<{ success: boolean }>('/settings', { method: 'PUT', body: JSON.stringify(patch) });
