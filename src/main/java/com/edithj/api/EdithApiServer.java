package com.edithj.api;

import com.edithj.assistant.AssistantResponse;
import com.edithj.assistant.AssistantService;
import com.edithj.assistant.AssistantTelemetry;
import com.edithj.config.PreferencesService;
import com.edithj.desktop.ClipboardService;
import com.edithj.desktop.DesktopFileService;
import com.edithj.desktop.SystemClipboardService;
import com.edithj.desktop.SystemDesktopFileService;
import com.edithj.notes.Note;
import com.edithj.notes.NoteService;
import com.edithj.reminders.Reminder;
import com.edithj.reminders.ReminderService;
import com.edithj.storage.RepositoryFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wires all EDITH features to REST endpoints consumed by the React frontend.
 */
public final class EdithApiServer {


    private final NoteService noteService;
    private final ReminderService reminderService;
    private final AssistantService assistantService;
    private final ClipboardService clipboardService;
    private final DesktopFileService desktopFileService;
    private final PreferencesService preferences;
    private final ObjectMapper mapper;

    public EdithApiServer() {
        this.noteService = new NoteService(RepositoryFactory.createNoteRepository());
        this.reminderService = new ReminderService(RepositoryFactory.createReminderRepository());
        this.assistantService = new AssistantService();
        this.clipboardService = new SystemClipboardService();
        this.desktopFileService = new SystemDesktopFileService();
        this.preferences = PreferencesService.instance();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Javalin createApp() {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            config.jsonMapper(new io.javalin.json.JavalinJackson(mapper, false));
        });

        registerRoutes(app);
        return app;
    }

    private void registerRoutes(Javalin app) {
        // ── Health ────────────────────────────────────────────────────────────
        app.get("/api/health", ctx -> ctx.json(Map.of("status", "ok", "service", "EDITH-J")));

        // ── Chat / Assistant ──────────────────────────────────────────────────
        app.post("/api/chat", ctx -> {
            ChatRequest req = ctx.bodyAsClass(ChatRequest.class);
            if (req.message() == null || req.message().isBlank()) {
                ctx.status(400).json(Map.of("error", "message is required"));
                return;
            }
            AssistantResponse response = assistantService.handleTypedInput(req.message());
            ctx.json(new ChatMessageDto("edith", response.answer(), Instant.now().toString()));
        });

        // ── Notes ─────────────────────────────────────────────────────────────
        app.get("/api/notes", ctx -> {
            String q = ctx.queryParam("q");
            List<Note> notes = (q != null && !q.isBlank())
                    ? noteService.searchNotes(q)
                    : noteService.listNotes();
            ctx.json(notes);
        });

        app.post("/api/notes", ctx -> {
            NoteRequest req = ctx.bodyAsClass(NoteRequest.class);
            Note created = noteService.createNote(req.content());
            ctx.status(201).json(created);
        });

        app.put("/api/notes/{id}", ctx -> {
            String id = ctx.pathParam("id");
            NoteRequest req = ctx.bodyAsClass(NoteRequest.class);
            Optional<Note> updated = noteService.updateNote(id, req.content());
            if (updated.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Note not found"));
                return;
            }
            ctx.json(updated.get());
        });

        app.delete("/api/notes/{id}", ctx -> {
            String id = ctx.pathParam("id");
            boolean deleted = noteService.deleteNote(id);
            if (!deleted) {
                ctx.status(404).json(Map.of("error", "Note not found"));
                return;
            }
            ctx.status(204);
        });

        // ── Reminders ─────────────────────────────────────────────────────────
        app.get("/api/reminders", ctx -> ctx.json(reminderService.listReminders()));

        app.post("/api/reminders", ctx -> {
            ReminderRequest req = ctx.bodyAsClass(ReminderRequest.class);
            try {
                Reminder created = reminderService.createReminder(req.text(), req.dueHint());
                ctx.status(201).json(created);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("error", e.getMessage()));
            }
        });

        app.post("/api/reminders/{id}/complete", ctx -> {
            String id = ctx.pathParam("id");
            Optional<Reminder> done = reminderService.markDone(id);
            if (done.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Reminder not found"));
                return;
            }
            ctx.json(Map.of("success", true));
        });

        app.delete("/api/reminders/{id}", ctx -> {
            String id = ctx.pathParam("id");
            boolean deleted = reminderService.deleteReminder(id);
            if (!deleted) {
                ctx.status(404).json(Map.of("error", "Reminder not found"));
                return;
            }
            ctx.status(204);
        });

        // ── Clipboard ─────────────────────────────────────────────────────────
        app.get("/api/clipboard", ctx -> ctx.json(Map.of("text", clipboardService.readText())));

        app.post("/api/clipboard", ctx -> {
            ClipboardRequest req = ctx.bodyAsClass(ClipboardRequest.class);
            boolean ok = clipboardService.writeText(req.text());
            ctx.json(Map.of("success", ok));
        });

        // ── Recent Files ──────────────────────────────────────────────────────
        app.get("/api/files/recent", ctx -> {
            List<String> paths = desktopFileService.listRecentFiles(20).stream()
                    .map(Path::toString)
                    .toList();
            ctx.json(paths);
        });

        // ── Telemetry ─────────────────────────────────────────────────────────
        app.get("/api/telemetry", ctx -> {
            AssistantTelemetry.Snapshot snap = AssistantTelemetry.instance().snapshot();
            ctx.json(Map.of(
                    "clarificationPrompts", snap.clarificationPrompts(),
                    "worldCircuitOpenHits", snap.worldCircuitOpenHits(),
                    "localKbEmptyHits", snap.localKbEmptyHits()
            ));
        });

        app.post("/api/telemetry/reset", ctx -> {
            AssistantTelemetry.instance().reset();
            ctx.json(Map.of("success", true));
        });

        // ── Settings / Preferences ────────────────────────────────────────────
        app.get("/api/settings", ctx -> ctx.json(Map.of(
                "autoSendVoiceInput", preferences.isAutoSendVoiceInputEnabled(),
                "preferShortcutApps", preferences.isPreferShortcutAppsEnabled(),
                "allowWebFallback", preferences.isWebFallbackAllowed(),
                "whatsappAppFirst", preferences.isWhatsAppAppFirstEnabled(),
                "devSmokeLaunchersEnabled", preferences.isDevSmokeLaunchersEnabled()
        )));

        app.put("/api/settings", ctx -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body.containsKey("autoSendVoiceInput"))
                preferences.setAutoSendVoiceInputEnabled((Boolean) body.get("autoSendVoiceInput"));
            if (body.containsKey("preferShortcutApps"))
                preferences.setPreferShortcutAppsEnabled((Boolean) body.get("preferShortcutApps"));
            if (body.containsKey("allowWebFallback"))
                preferences.setWebFallbackAllowed((Boolean) body.get("allowWebFallback"));
            if (body.containsKey("whatsappAppFirst"))
                preferences.setWhatsAppAppFirstEnabled((Boolean) body.get("whatsappAppFirst"));
            if (body.containsKey("devSmokeLaunchersEnabled"))
                preferences.setDevSmokeLaunchersEnabled((Boolean) body.get("devSmokeLaunchersEnabled"));
            ctx.json(Map.of("success", true));
        });

        // SPA fallback — serve index.html for all unmatched routes
        app.error(404, ctx -> {
            if (!ctx.path().startsWith("/api")) {
                ctx.redirect("/");
            }
        });
    }

    // ── Request / Response DTOs ────────────────────────────────────────────────
    public record ChatRequest(String message) {}
    public record ChatMessageDto(String role, String content, String timestamp) {}
    public record NoteRequest(String content) {}
    public record ReminderRequest(String text, String dueHint) {}
    public record ClipboardRequest(String text) {}
}
