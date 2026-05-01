import { useState, useEffect, useRef, useCallback } from 'react';
import type { AssistantResponse, ChatMessage, Note, Reminder, Settings, Telemetry } from './api';
import * as api from './api';
import './App.css';

type View = 'chat' | 'notes' | 'reminders' | 'desktop' | 'settings';

/* ───────────────── helpers ─────────────────────── */
function fmt(iso: string) {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

function toAssistantMessage(response: AssistantResponse): ChatMessage {
  return {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    role: 'edith',
    content: response.answer,
    timestamp: new Date().toISOString(),
    source: response.source,
    intentType: response.intentType,
    success: response.success,
    requiresApproval: response.requiresApproval,
    approvalType: response.approvalType,
    explanation: response.explanation,
    taskPlan: response.taskPlan,
    actions: response.actions,
    recoveryOptions: response.recoveryOptions,
  };
}

function isPlanComplete(message?: ChatMessage | null) {
  const steps = message?.taskPlan?.steps ?? [];
  return steps.length > 0 && steps.every(step => step.status.toLowerCase() === 'done');
}

function planStepClass(status?: string) {
  const normalized = (status ?? '').toLowerCase();
  if (normalized === 'done') return 'task-step done';
  if (normalized === 'in_progress') return 'task-step in-progress';
  return 'task-step pending';
}

const BOOT_SEQ = [
  '> EDITH-J v2.0 INITIALIZING...',
  '> Loading AI subsystems...',
  '> Connecting to backend REST API...',
  '> Verifying SQLite storage...',
  '> Voice pipeline standing by...',
  '> All systems nominal.',
  '> Welcome back.',
];

/* ═══════════════════════════════════════════════════
   MAIN APP
═══════════════════════════════════════════════════ */
export default function App() {
  const [view, setView] = useState<View>('chat');
  const [booted, setBooted] = useState(false);
  const [bootLines, setBootLines] = useState<string[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);

  useEffect(() => {
    let i = 0;
    const id = setInterval(() => {
      setBootLines(prev => [...prev, BOOT_SEQ[i]]);
      i++;
      if (i >= BOOT_SEQ.length) {
        clearInterval(id);
        setTimeout(() => setBooted(true), 600);
      }
    }, 260);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    if (booted) {
      api.getChatHistory()
        .then(history => {
          if (history && history.length > 0) {
            setMessages(history);
          } else {
            setMessages([{
              id: 'boot-greeting',
              role: 'edith',
              content: 'EDITH Initialization Complete. All systems nominal. How may I assist?',
              timestamp: new Date().toISOString(),
              source: 'System',
              intentType: 'GENERAL_CHAT',
              success: true,
            }]);
          }
        })
        .catch(err => console.error("Failed to load chat history:", err));
    }
  }, [booted]);

  if (!booted) {
    return (
      <div className="boot-screen">
        <div className="boot-logo">E·D·I·T·H</div>
        <div className="boot-sub">Extended Digital Intelligence & Threat Handler</div>
        <div className="boot-log">
          {bootLines.map((l, i) => <div key={i} className="boot-line">{l}</div>)}
        </div>
      </div>
    );
  }

  const navItems: { id: View; label: string; icon: string }[] = [
    { id: 'chat',      label: 'CHAT',      icon: '💬' },
    { id: 'notes',     label: 'NOTES',     icon: '📝' },
    { id: 'reminders', label: 'REMINDERS', icon: '⏰' },
    { id: 'desktop',   label: 'DESKTOP',   icon: '🖥' },
    { id: 'settings',  label: 'SETTINGS',  icon: '⚙️' },
  ];

  return (
    <div className="shell">
      {/* ── Sidebar ── */}
      <aside className="sidebar">
        <div className="sidebar-logo">E·D·I·T·H</div>
        <nav className="sidebar-nav">
          {navItems.map(n => (
            <button
              key={n.id}
              className={`nav-btn${view === n.id ? ' active' : ''}`}
              onClick={() => setView(n.id)}
              title={n.label}
            >
              <span className="nav-icon">{n.icon}</span>
              <span className="nav-label">{n.label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-footer">
          <span className="status-dot" />
          <span className="status-text">ONLINE</span>
        </div>
      </aside>

      {/* ── Main Content ── */}
      <main className="main-content">
        {view === 'chat'      && <ChatView messages={messages} setMessages={setMessages} />}
        {view === 'notes'     && <NotesView />}
        {view === 'reminders' && <RemindersView />}
        {view === 'desktop'   && <DesktopView />}
        {view === 'settings'  && <SettingsView />}
      </main>
    </div>
  );
}

/* ═══════════════════════════════════════════════════
   CHAT VIEW
═══════════════════════════════════════════════════ */
function ChatView({ messages, setMessages }: { messages: ChatMessage[], setMessages: React.Dispatch<React.SetStateAction<ChatMessage[]>> }) {
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [listening, setListening] = useState(false);
  const [status, setStatus] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);
  const activePlanMessage = [...messages].reverse().find(message => message.role !== 'user' && message.taskPlan && !isPlanComplete(message));

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages]);

  const submitMessage = useCallback(async (rawText: string) => {
    const text = rawText.trim();
    if (!text || loading) return;
    setMessages(prev => [...prev, {
      id: `${Date.now()}-user`,
      role: 'user',
      content: text,
      timestamp: new Date().toISOString(),
      source: 'User',
      success: true,
    }]);
    setLoading(true);
    try {
      const resp = await api.sendChat(text);
      setMessages(prev => [...prev, toAssistantMessage(resp)]);
    } catch (e: unknown) {
      setMessages(prev => [...prev, {
        id: `${Date.now()}-error`,
        role: 'edith',
        content: `Error: ${(e as Error).message}`,
        timestamp: new Date().toISOString(),
        source: 'System',
        success: false,
        }]);
    } finally {
      setLoading(false);
    }
  }, [loading, setMessages]);

  const send = useCallback(async () => {
    const text = input.trim();
    if (!text || loading) return;
    setInput('');
    await submitMessage(text);
  }, [input, loading, submitMessage]);

  const toggleVoice = async () => {
    if (listening) {
      setListening(false);
      setLoading(true);
      try {
        const res = await api.stopVoice();
        if (res.transcript) {
          setMessages(prev => [...prev, {
            id: `${Date.now()}-voice-user`,
            role: 'user',
            content: res.transcript,
            timestamp: new Date().toISOString(),
            source: 'User',
            success: true,
          }]);
          setMessages(prev => [...prev, toAssistantMessage(res.response)]);
        }
      } catch (e: unknown) {
        setMessages(prev => [...prev, {
          id: `${Date.now()}-voice-error`,
          role: 'edith',
          content: `Voice Error: ${(e as Error).message}`,
          timestamp: new Date().toISOString(),
          source: 'System',
          success: false,
        }]);
      } finally {
        setLoading(false);
      }
    } else {
      try {
        await api.startVoice();
        setListening(true);
      } catch (e: unknown) {
        setMessages(prev => [...prev, {
          id: `${Date.now()}-voice-start-error`,
          role: 'edith',
          content: `Voice Error: ${(e as Error).message}`,
          timestamp: new Date().toISOString(),
          source: 'System',
          success: false,
        }]);
      }
    }
  };

  const clearChat = async () => {
    if (loading || listening) return;
    const confirmed = window.confirm('Clear the current chat view? The conversation will be archived to memory first.');
    if (!confirmed) return;
    setLoading(true);
    try {
      const result = await api.clearChatHistory();
      setMessages([{
        id: 'chat-cleared',
        role: 'edith',
        content: 'Chat cleared. I archived the conversation to memory for future reference.',
        timestamp: new Date().toISOString(),
        source: 'Memory',
        intentType: 'GENERAL_CHAT',
        success: true,
      }]);
      setStatus(result.archivedMessages > 0
        ? `Archived ${result.archivedMessages} messages to memory.`
        : 'Chat was already empty.');
      setTimeout(() => setStatus(''), 4000);
    } catch (e: unknown) {
      setMessages(prev => [...prev, {
        id: `${Date.now()}-clear-error`,
        role: 'edith',
        content: `Error: ${(e as Error).message}`,
        timestamp: new Date().toISOString(),
        source: 'System',
        success: false,
      }]);
    } finally {
      setLoading(false);
    }
  };

  const QUICK = ['What can you do?', 'Set a reminder', 'Show recent files', 'Take a note'];

  return (
    <div className="view chat-view">
      <div className="view-header">
        <h1>CHAT</h1>
        <span className="view-sub">AI Assistant</span>
        <button className="header-btn" onClick={clearChat} disabled={loading || listening}>CLEAR CHAT</button>
      </div>

      {status && <div className="status-bar">{status}</div>}

      <div className="quick-actions">
        {QUICK.map(q => (
          <button key={q} className="quick-btn" onClick={() => { setInput(q); }}>
            {q}
          </button>
        ))}
      </div>

      {activePlanMessage?.taskPlan && (
        <div className="active-plan-banner">
          <div className="active-plan-copy">
            <span className="active-plan-label">ACTIVE PLAN</span>
            <div className="active-plan-goal">{activePlanMessage.taskPlan.goal}</div>
          </div>
          <div className="active-plan-actions">
            <button
              className="msg-action-btn"
              disabled={loading || listening}
              onClick={() => submitMessage('show plan status')}
            >
              Show status
            </button>
            <button
              className="msg-action-btn"
              disabled={loading || listening}
              onClick={() => submitMessage('continue this plan')}
            >
              Continue
            </button>
          </div>
        </div>
      )}

      <div className="message-list">
        {messages.map((m, i) => (
          <div key={m.id ?? i} className={`message ${m.role}`}>
            <div className="msg-header">
              <span className="msg-role">{m.role === 'user' ? '▸ YOU' : '▸ EDITH'}</span>
              {m.role !== 'user' && m.source && <span className={`source-badge ${m.success === false ? 'error' : ''}`}>{m.source}</span>}
              {m.role !== 'user' && m.requiresApproval && <span className="pending-badge">PENDING APPROVAL</span>}
              <span className="msg-ts">{fmt(m.timestamp)}</span>
            </div>
            <div className="msg-body">{m.content}</div>
            {m.role !== 'user' && m.explanation && (
              <div className={`msg-explanation ${m.requiresApproval ? 'approval' : m.success === false ? 'error' : ''}`}>
                {m.explanation}
              </div>
            )}
            {m.role !== 'user' && m.intentType && (
              <div className="msg-meta">
                <span>{m.intentType.replaceAll('_', ' ')}</span>
                {m.approvalType && <span> · {m.approvalType.replaceAll('_', ' ')}</span>}
              </div>
            )}
            {m.role !== 'user' && m.taskPlan && (
              <div className="task-plan">
                <div className="task-plan-goal">{m.taskPlan.goal}</div>
                <div className="task-plan-steps">
                  {m.taskPlan.steps.map(step => (
                    <div key={step.id} className={planStepClass(step.status)}>
                      <div className="task-step-title">{step.title}</div>
                      <div className="task-step-meta">{step.tool} · {step.status}</div>
                      <div className="task-step-detail">{step.detail}</div>
                    </div>
                  ))}
                </div>
              </div>
            )}
            {m.role !== 'user' && ((m.actions?.length ?? 0) > 0 || (m.recoveryOptions?.length ?? 0) > 0) && (
              <div className="msg-actions">
                {m.actions?.map(action => (
                  <button
                    key={action.id || `${m.id}-${action.label}`}
                    className="msg-action-btn"
                    disabled={loading || listening}
                    onClick={() => submitMessage(action.value)}
                    title={action.kind || action.label}
                  >
                    {action.label}
                  </button>
                ))}
                {m.recoveryOptions?.map(option => (
                  <button
                    key={`${m.id}-${option.label}`}
                    className="msg-action-btn secondary"
                    disabled={loading || listening}
                    onClick={() => submitMessage(option.prompt)}
                    title={option.prompt}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            )}
          </div>
        ))}
        {loading && (
          <div className="message edith">
            <div className="msg-header"><span className="msg-role">▸ EDITH</span></div>
            <div className="msg-body thinking">
              <span /><span /><span />
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <div className="chat-input-bar">
        <div className="input-accent" />
        <button
          className={`mic-btn${listening ? ' active' : ''}`}
          onClick={toggleVoice}
          title={listening ? "Stop listening" : "Start voice command"}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
            <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
            <line x1="12" y1="19" x2="12" y2="23" />
            <line x1="8" y1="23" x2="16" y2="23" />
          </svg>
        </button>
        <input
          className="chat-input"
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && !e.shiftKey && send()}
          placeholder={listening ? "Listening..." : "Awaiting command..."}
          disabled={listening}
        />
        <button
          className="send-btn"
          onClick={send}
          disabled={loading || listening || !input.trim()}
          aria-label="Send message"
          title="Send message"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="22" y1="2" x2="11" y2="13" />
            <polygon points="22 2 15 22 11 13 2 9 22 2" />
          </svg>
        </button>
      </div>
    </div>
  );
}

/* ═══════════════════════════════════════════════════
   NOTES VIEW
═══════════════════════════════════════════════════ */
function NotesView() {
  const [notes, setNotes] = useState<Note[]>([]);
  const [query, setQuery]   = useState('');
  const [selected, setSelected] = useState<Note | null>(null);
  const [draft, setDraft]   = useState('');
  const [creating, setCreating] = useState(false);
  const [newContent, setNewContent] = useState('');
  const [error, setError] = useState('');

  // Stable ref so handleSearch can trigger a reload without re-running the initial effect
  const loadNotes = useCallback(async (q?: string) => {
    try { setNotes(await api.getNotes(q)); } catch (e: unknown) { setError((e as Error).message); }
  }, []);

  useEffect(() => {
    let cancelled = false;
    api.getNotes()
      .then(data  => { if (!cancelled) setNotes(data); })
      .catch(err  => { if (!cancelled) setError((err as Error).message); });
    return () => { cancelled = true; };
  }, []);

  const handleSearch = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter') loadNotes(query);
  }, [loadNotes, query]);

  const handleSelect = (n: Note) => { setSelected(n); setDraft(n.content); };

  const handleSave = async () => {
    if (!selected) return;
    try {
      const updated = await api.updateNote(selected.id, draft);
      setSelected(updated);
      setNotes(prev => prev.map(n => n.id === updated.id ? updated : n));
    } catch (e: unknown) { setError((e as Error).message); }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.deleteNote(id);
      setNotes(prev => prev.filter(n => n.id !== id));
      if (selected?.id === id) setSelected(null);
    } catch (e: unknown) { setError((e as Error).message); }
  };

  const handleCreate = async () => {
    if (!newContent.trim()) return;
    try {
      const note = await api.createNote(newContent);
      setNotes(prev => [note, ...prev]);
      setNewContent(''); setCreating(false);
    } catch (e: unknown) { setError((e as Error).message); }
  };

  return (
    <div className="view notes-view">
      <div className="view-header">
        <h1>NOTES</h1>
        <button className="header-btn" onClick={() => setCreating(c => !c)}>+ NEW NOTE</button>
      </div>

      {error && <div className="error-bar">{error} <button onClick={() => setError('')}>✕</button></div>}

      {creating && (
        <div className="create-panel">
          <textarea className="create-input" rows={4} placeholder="Write your note..." value={newContent} onChange={e => setNewContent(e.target.value)} />
          <div className="create-actions">
            <button className="action-btn primary" onClick={handleCreate}>Save</button>
            <button className="action-btn" onClick={() => { setCreating(false); setNewContent(''); }}>Cancel</button>
          </div>
        </div>
      )}

      <div className="search-bar">
        <input className="search-input" placeholder="Search notes..." value={query} onChange={e => setQuery(e.target.value)} onKeyDown={handleSearch} />
        <button className="search-btn" onClick={() => loadNotes(query)} title="Search">🔍</button>
      </div>

      <div className="notes-layout">
        <div className="notes-list">
          {notes.length === 0 && <div className="empty-state">No notes yet. Create one!</div>}
          {notes.map(n => (
            <div key={n.id} className={`note-card${selected?.id === n.id ? ' selected' : ''}`} onClick={() => handleSelect(n)}>
              <div className="note-title">{n.title || 'Untitled'}</div>
              <div className="note-preview">{n.content.substring(0, 80)}{n.content.length > 80 ? '...' : ''}</div>
              <div className="note-meta">{fmt(n.updatedAt)}</div>
              <button className="delete-btn" onClick={e => { e.stopPropagation(); handleDelete(n.id); }} title="Delete note">✕</button>
            </div>
          ))}
        </div>

        {selected && (
          <div className="note-editor">
            <div className="editor-header">
              <span className="editor-title">{selected.title}</span>
              <button className="action-btn primary" onClick={handleSave}>Save Changes</button>
            </div>
            <textarea className="editor-area" value={draft} onChange={e => setDraft(e.target.value)} placeholder="Write your note here..." />
            <div className="editor-meta">Created: {fmt(selected.createdAt)} · Updated: {fmt(selected.updatedAt)}</div>
          </div>
        )}
      </div>
    </div>
  );
}

/* ═══════════════════════════════════════════════════
   REMINDERS VIEW
═══════════════════════════════════════════════════ */
function RemindersView() {
  const [reminders, setReminders] = useState<Reminder[]>([]);
  const [text, setText]   = useState('');
  const [dueHint, setDue] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    api.getReminders()
      .then(data  => { if (!cancelled) setReminders(data); })
      .catch(err  => { if (!cancelled) setError((err as Error).message); });
    return () => { cancelled = true; };
  }, []);

  const handleCreate = async () => {
    if (!text.trim() || !dueHint.trim()) { setError('Both text and due time are required.'); return; }
    try {
      const r = await api.createReminder(text, dueHint);
      setReminders(prev => [r, ...prev]);
      setText(''); setDue('');
    } catch (e: unknown) { setError((e as Error).message); }
  };

  const handleComplete = async (id: string) => {
    try {
      await api.completeReminder(id);
      setReminders(prev => prev.filter(r => r.id !== id));
    } catch (e: unknown) { setError((e as Error).message); }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.deleteReminder(id);
      setReminders(prev => prev.filter(r => r.id !== id));
    } catch (e: unknown) { setError((e as Error).message); }
  };

  return (
    <div className="view reminders-view">
      <div className="view-header"><h1>REMINDERS</h1></div>

      {error && <div className="error-bar">{error} <button onClick={() => setError('')}>✕</button></div>}

      <div className="create-panel">
        <div className="create-row">
          <input className="field-input" placeholder="Reminder text..." value={text} onChange={e => setText(e.target.value)} />
          <input className="field-input" placeholder="Due time (e.g. in 30 minutes, tomorrow 9am)" value={dueHint} onChange={e => setDue(e.target.value)} />
          <button className="action-btn primary" onClick={handleCreate}>+ Add</button>
        </div>
      </div>

      <div className="reminders-list">
        {reminders.length === 0 && <div className="empty-state">No pending reminders. Add one!</div>}
        {reminders.map(r => (
          <div key={r.id} className="reminder-card">
            <div className="reminder-info">
              <div className="reminder-text">{r.text}</div>
              <div className="reminder-due">⏰ {r.dueAt ? fmt(r.dueAt) : 'No due time'}</div>
            </div>
            <div className="reminder-actions">
              <button className="action-btn primary small" onClick={() => handleComplete(r.id)} title="Mark as done">✓ Done</button>
              <button className="action-btn small" onClick={() => handleDelete(r.id)} title="Delete reminder">✕</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ═══════════════════════════════════════════════════
   DESKTOP TOOLS VIEW
═══════════════════════════════════════════════════ */
function DesktopView() {
  const [clipboard, setClipboard] = useState('');
  const [clipboardWrite, setClipboardWrite] = useState('');
  const [files, setFiles]           = useState<string[]>([]);
  const [telemetry, setTelemetry]   = useState<Telemetry | null>(null);
  const [status, setStatus]         = useState('');
  const [error, setError]           = useState('');

  const msg = (m: string) => { setStatus(m); setTimeout(() => setStatus(''), 3000); };

  useEffect(() => {
    api.getClipboard().then(r => setClipboard(r.text)).catch(() => {});
    api.getRecentFiles().then(setFiles).catch(() => {});
    api.getTelemetry().then(setTelemetry).catch(() => {});
  }, []);

  const handleReadClipboard = async () => {
    try { const r = await api.getClipboard(); setClipboard(r.text); msg('Clipboard read.'); }
    catch (e: unknown) { setError((e as Error).message); }
  };

  const handleWriteClipboard = async () => {
    try { await api.writeClipboard(clipboardWrite); msg('Copied to clipboard!'); setClipboardWrite(''); }
    catch (e: unknown) { setError((e as Error).message); }
  };

  const handleResetTelemetry = async () => {
    try {
      await api.resetTelemetry();
      const t = await api.getTelemetry();
      setTelemetry(t);
      msg('Telemetry reset.');
    } catch (e: unknown) { setError((e as Error).message); }
  };

  return (
    <div className="view desktop-view">
      <div className="view-header"><h1>DESKTOP TOOLS</h1></div>

      {error  && <div className="error-bar">{error} <button onClick={() => setError('')}>✕</button></div>}
      {status && <div className="status-bar">{status}</div>}

      <div className="tools-grid">
        {/* Clipboard */}
        <div className="tool-card">
          <h2 className="tool-title">📋 CLIPBOARD</h2>
          <div className="clipboard-current">
            <span className="tool-label">Current Contents:</span>
            <div className="clipboard-text">{clipboard || '(empty)'}</div>
            <button className="action-btn small" onClick={handleReadClipboard} title="Refresh clipboard">↻ Refresh</button>
          </div>
          <div className="clipboard-write">
            <input className="field-input" placeholder="Write to clipboard..." value={clipboardWrite} onChange={e => setClipboardWrite(e.target.value)} />
            <button className="action-btn primary small" onClick={handleWriteClipboard} title="Write to clipboard">Copy</button>
          </div>
        </div>

        {/* Recent Files */}
        <div className="tool-card">
          <h2 className="tool-title">📁 RECENT FILES</h2>
          {files.length === 0 && <div className="empty-state small">No recent files found.</div>}
          <ul className="file-list">
            {files.slice(0, 12).map((f, i) => (
              <li key={i} className="file-item" title={f}>
                <span className="file-icon">📄</span>
                <span className="file-name">{f.split(/[/\\]/).pop()}</span>
                <span className="file-dir">{f.split(/[/\\]/).slice(0, -1).join('/').substring(0, 40)}...</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Telemetry */}
        <div className="tool-card">
          <h2 className="tool-title">📊 TELEMETRY</h2>
          {telemetry && (
            <div className="telemetry-grid">
              <div className="tele-item">
                <div className="tele-value">{telemetry.clarificationPrompts}</div>
                <div className="tele-label">Clarification Prompts</div>
              </div>
              <div className="tele-item">
                <div className="tele-value">{telemetry.worldCircuitOpenHits}</div>
                <div className="tele-label">World Circuit Opens</div>
              </div>
              <div className="tele-item">
                <div className="tele-value">{telemetry.localKbEmptyHits}</div>
                <div className="tele-label">Local KB Misses</div>
              </div>
            </div>
          )}
          <button className="action-btn small" onClick={handleResetTelemetry} title="Reset telemetry counters">↺ Reset Telemetry</button>
        </div>
      </div>
    </div>
  );
}

/* Renders aria-checked with static literal values to satisfy strict ARIA linters. */
function ToggleSwitch({ checked, onToggle, label }: { checked: boolean; onToggle: () => void; label: string }) {
  if (checked) {
    return (
      <button className="toggle-btn on" onClick={onToggle} role="switch" aria-checked="true" title={label}>
        <span className="toggle-knob" />
      </button>
    );
  }
  return (
    <button className="toggle-btn" onClick={onToggle} role="switch" aria-checked="false" title={label}>
      <span className="toggle-knob" />
    </button>
  );
}

/* ═══════════════════════════════════════════════════
   SETTINGS VIEW
═══════════════════════════════════════════════════ */
function SettingsView() {
  const [settings, setSettings] = useState<Settings | null>(null);
  const [saved, setSaved]       = useState(false);
  const [error, setError]       = useState('');

  useEffect(() => {
    api.getSettings().then(setSettings).catch((e: unknown) => setError((e as Error).message));
  }, []);

  const toggle = async (key: keyof Settings) => {
    if (!settings) return;
    const updated = { ...settings, [key]: !settings[key] };
    setSettings(updated);
    try { await api.updateSettings({ [key]: updated[key] }); setSaved(true); setTimeout(() => setSaved(false), 2000); }
    catch (e: unknown) { setError((e as Error).message); }
  };

  const ITEMS: { key: keyof Settings; label: string; desc: string }[] = [
    { key: 'autoSendVoiceInput',       label: 'Auto-send voice input',    desc: 'Automatically send recognized speech as a message.' },
    { key: 'preferShortcutApps',       label: 'Prefer shortcut apps',     desc: 'Open apps via OS shortcuts instead of web fallbacks.' },
    { key: 'allowWebFallback',         label: 'Allow web fallback',       desc: 'Fall back to web URLs when app launch fails.' },
    { key: 'whatsappAppFirst',         label: 'WhatsApp app first',       desc: 'Try the WhatsApp desktop app before WhatsApp Web.' },
    { key: 'devSmokeLaunchersEnabled', label: 'Dev: Smoke launchers',     desc: 'Enable smoke-test launcher commands (developer mode).' },
  ];

  return (
    <div className="view settings-view">
      <div className="view-header">
        <h1>SETTINGS</h1>
        {saved && <span className="saved-badge">✓ Saved</span>}
      </div>

      {error && <div className="error-bar">{error} <button onClick={() => setError('')}>✕</button></div>}

      {!settings ? (
        <div className="loading">Loading settings...</div>
      ) : (
        <div className="settings-list">
          {ITEMS.map(item => (
            <div key={item.key} className="setting-row">
              <div className="setting-info">
                <div className="setting-label">{item.label}</div>
                <div className="setting-desc">{item.desc}</div>
              </div>
              <ToggleSwitch
                checked={settings[item.key]}
                onToggle={() => toggle(item.key)}
                label={item.label}
              />
            </div>
          ))}
        </div>
      )}

      <div className="settings-info">
        <div className="info-card">
          <h3>API ENDPOINT</h3>
          <code>http://localhost:8080/api</code>
        </div>
        <div className="info-card">
          <h3>BACKEND</h3>
          <code>EDITH-J Java/Javalin</code>
        </div>
        <div className="info-card">
          <h3>FRONTEND</h3>
          <code>React + Vite + TypeScript</code>
        </div>
      </div>
    </div>
  );
}
