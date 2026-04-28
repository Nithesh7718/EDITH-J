package com.edithj.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.api.dto.ChatRequest;
import com.edithj.api.dto.ChatResponse;
import com.edithj.api.dto.NoteRequest;
import com.edithj.api.dto.ReminderRequest;
import com.edithj.assistant.AssistantResponse;
import com.edithj.assistant.AssistantService;
import com.edithj.assistant.AssistantStatus;
import com.edithj.assistant.AssistantStatusService;
import com.edithj.notes.Note;
import com.edithj.notes.NoteService;
import com.edithj.reminders.Reminder;
import com.edithj.reminders.ReminderService;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.bundled.CorsPluginConfig;

public final class ApiServer {

    private static final Logger logger = LoggerFactory.getLogger(ApiServer.class);

    private ApiServer() {
    }

    public static Javalin create(
            AssistantService assistantService,
            NoteService noteService,
            ReminderService reminderService,
            AssistantStatusService statusService) {

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors ->
                cors.addRule(CorsPluginConfig.CorsRule::anyHost));
            config.staticFiles.add("/public");
        });

        registerExceptionHandlers(app);
        registerRoutes(app, assistantService, noteService, reminderService, statusService);

        return app;
    }

    private static void registerExceptionHandlers(Javalin app) {
        app.exception(IllegalArgumentException.class, (ex, ctx) -> {
            logger.debug("Bad request: {}", ex.getMessage());
            ctx.status(400).json(Map.of("error", ex.getMessage()));
        });
        app.exception(Exception.class, (ex, ctx) -> {
            logger.error("Unhandled exception", ex);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        });
    }

    private static void registerRoutes(
            Javalin app,
            AssistantService assistantService,
            NoteService noteService,
            ReminderService reminderService,
            AssistantStatusService statusService) {

        // ── Status ──────────────────────────────────────────────────────────
        app.get("/api/status", ctx -> {
            AssistantStatus status = statusService.status();
            ctx.json(Map.of(
                "status", status.name(),
                "message", statusService.message()
            ));
        });

        // ── Chat ─────────────────────────────────────────────────────────────
        app.post("/api/chat", ctx -> {
            ChatRequest req = ctx.bodyAsClass(ChatRequest.class);
            if (req.message() == null || req.message().isBlank()) {
                throw new BadRequestResponse("message must not be blank");
            }
            AssistantResponse response = assistantService.handleTypedInput(req.message());
            ctx.status(200).json(new ChatResponse(response.answer(), response.intentType().name()));
        });

        // ── Notes ─────────────────────────────────────────────────────────────
        app.get("/api/notes", ctx -> {
            String query = ctx.queryParam("q");
            List<Note> notes = (query != null && !query.isBlank())
                    ? noteService.searchNotes(query)
                    : noteService.listNotes();
            ctx.json(notes);
        });

        app.post("/api/notes", ctx -> {
            NoteRequest req = ctx.bodyAsClass(NoteRequest.class);
            if (req.content() == null || req.content().isBlank()) {
                throw new BadRequestResponse("content must not be blank");
            }
            Note note = noteService.createNote(req.content());
            ctx.status(201).json(note);
        });

        app.put("/api/notes/{id}", ctx -> {
            String id = ctx.pathParam("id");
            NoteRequest req = ctx.bodyAsClass(NoteRequest.class);
            if (req.content() == null || req.content().isBlank()) {
                throw new BadRequestResponse("content must not be blank");
            }
            Note note = noteService.updateNote(id, req.content())
                    .orElseThrow(() -> new NotFoundResponse("Note not found: " + id));
            ctx.json(note);
        });

        app.delete("/api/notes/{id}", ctx -> {
            String id = ctx.pathParam("id");
            boolean deleted = noteService.deleteNote(id);
            if (!deleted) {
                throw new NotFoundResponse("Note not found: " + id);
            }
            ctx.status(204);
        });

        // ── Reminders ─────────────────────────────────────────────────────────
        app.get("/api/reminders", ctx -> {
            String query = ctx.queryParam("q");
            boolean all = Boolean.parseBoolean(ctx.queryParam("all"));
            List<Reminder> reminders;
            if (all) {
                reminders = reminderService.listAllReminders();
            } else if (query != null && !query.isBlank()) {
                reminders = reminderService.searchReminders(query);
            } else {
                reminders = reminderService.listReminders();
            }
            ctx.json(reminders);
        });

        app.post("/api/reminders", ctx -> {
            ReminderRequest req = ctx.bodyAsClass(ReminderRequest.class);
            if (req.text() == null || req.text().isBlank()) {
                throw new BadRequestResponse("text must not be blank");
            }
            if (req.dueAt() == null || req.dueAt().isBlank()) {
                throw new BadRequestResponse("dueAt must not be blank");
            }
            Reminder reminder = reminderService.createReminder(req.text(), req.dueAt());
            ctx.status(201).json(reminder);
        });

        app.patch("/api/reminders/{id}/done", ctx -> {
            String id = ctx.pathParam("id");
            Reminder reminder = reminderService.markDone(id)
                    .orElseThrow(() -> new NotFoundResponse("Reminder not found: " + id));
            ctx.json(reminder);
        });

        app.delete("/api/reminders/{id}", ctx -> {
            String id = ctx.pathParam("id");
            boolean deleted = reminderService.deleteReminder(id);
            if (!deleted) {
                throw new NotFoundResponse("Reminder not found: " + id);
            }
            ctx.status(204);
        });
    }
}
