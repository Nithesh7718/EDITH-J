package com.edithj.commands;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import com.edithj.assistant.IntentType;
import com.edithj.launcher.AppLauncherService;
import com.edithj.notes.FileNoteRepository;
import com.edithj.notes.Note;
import com.edithj.notes.NoteService;
import com.edithj.reminders.FileReminderRepository;
import com.edithj.reminders.Reminder;
import com.edithj.reminders.ReminderService;

public class DesktopToolsCommandHandler implements CommandHandler {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final int MAX_ACTION_LOG = 30;
    private static final Set<String> TRUSTED_DOMAINS = Set.of(
            "google.com", "github.com", "stackoverflow.com", "wikipedia.org", "weather.com", "accuweather.com");

    private static final Deque<String> ACTION_LOG = new ArrayDeque<>();
    private static final List<TodoItem> TODO_ITEMS = new ArrayList<>();
    private static final Set<String> BLOCKED_DOMAINS = new HashSet<>();

    private static volatile String pendingUrlForConfirmation;
    private static volatile Instant focusEndsAt;

    private final AppLauncherService launcherService;
    private final NoteService noteService;
    private final ReminderService reminderService;

    private record TodoItem(int id, String text, boolean done) {

    }

    public DesktopToolsCommandHandler() {
        this(new AppLauncherService(), new NoteService(new FileNoteRepository()), new ReminderService(new FileReminderRepository()));
    }

    public DesktopToolsCommandHandler(AppLauncherService launcherService) {
        this(launcherService, new NoteService(new FileNoteRepository()), new ReminderService(new FileReminderRepository()));
    }

    public DesktopToolsCommandHandler(AppLauncherService launcherService, NoteService noteService, ReminderService reminderService) {
        this.launcherService = launcherService;
        this.noteService = noteService;
        this.reminderService = reminderService;
    }

    @Override
    public IntentType intentType() {
        return IntentType.DESKTOP_TOOLS;
    }

    @Override
    public String handle(CommandContext context) {
        String input = context == null ? "" : context.normalizedInput();
        String lower = input.toLowerCase(Locale.ROOT).trim();

        if (lower.isBlank() || isHelpRequest(lower)) {
            return capabilitySummary();
        }

        if (lower.contains("action log") || lower.contains("what did you do")) {
            return actionLogSummary();
        }

        if (lower.contains("daily briefing") || lower.contains("good morning")) {
            return dailyBriefing();
        }

        if (isSystemInfoRequest(lower)) {
            return systemStatus();
        }

        if (isClipboardRequest(lower)) {
            return handleClipboard(input, lower);
        }

        if (isRoutineRequest(lower)) {
            return handleRoutine(lower);
        }

        if (isDraftRequest(lower)) {
            return draftEmail(input);
        }

        if (isFileRequest(lower)) {
            return handleFiles(input, lower);
        }

        if (isTodoRequest(lower)) {
            return handleTasks(input, lower);
        }

        if (isFocusRequest(lower)) {
            return handleFocus(input, lower);
        }

        if (lower.startsWith("confirm open")) {
            return confirmOpen();
        }

        if (isWebSearchRequest(lower)) {
            if (lower.startsWith("open website") || lower.startsWith("open site")) {
                String candidate = extractSearchQuery(input);
                if (candidate.isBlank()) {
                    return "Tell me which website to open, for example: open website github.com.";
                }
                String url = normalizeUrl(candidate);
                if (isBlocked(url)) {
                    return "That site is blocked in focus mode.";
                }
                if (!isTrusted(url)) {
                    pendingUrlForConfirmation = url;
                    return "This looks untrusted. Say 'confirm open' to proceed: " + url;
                }
                String opened = launcherService.launchApp(url);
                logAction("Opened website " + url);
                return opened;
            }

            String query = extractSearchQuery(input);
            if (query.isBlank()) {
                return "Tell me what to search. Example: search web latest Java news.";
            }

            String searchUrl = "https://www.google.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            if (isBlocked(searchUrl)) {
                return "Web search blocked in focus mode for this domain.";
            }
            String openResult = launcherService.launchApp(searchUrl);
            logAction("Web search: " + query);
            return "Opened web search for: " + query + ". " + openResult;
        }

        return capabilitySummary();
    }

    private boolean isHelpRequest(String lower) {
        return lower.contains("help")
                || lower.contains("capabilities")
                || lower.contains("what can you do")
                || lower.equals("commands")
                || lower.equals("features");
    }

    private boolean isSystemInfoRequest(String lower) {
        return lower.contains("system info")
                || lower.contains("device info")
                || lower.contains("memory status")
                || lower.contains("status")
                || lower.contains("about this pc");
    }

    private boolean isWebSearchRequest(String lower) {
        return lower.startsWith("search web")
                || lower.startsWith("google ")
                || lower.startsWith("browse ")
                || lower.startsWith("open website")
                || lower.startsWith("open site");
    }

    private boolean isClipboardRequest(String lower) {
        return lower.contains("clipboard") || lower.startsWith("copy ");
    }

    private boolean isRoutineRequest(String lower) {
        return lower.startsWith("start work mode") || lower.startsWith("shutdown work mode") || lower.equals("work mode");
    }

    private boolean isDraftRequest(String lower) {
        return lower.startsWith("draft email") || lower.startsWith("write email") || lower.startsWith("compose email");
    }

    private boolean isFileRequest(String lower) {
        return lower.startsWith("find file") || lower.startsWith("open file") || lower.startsWith("open downloads")
                || lower.startsWith("recent files");
    }

    private boolean isTodoRequest(String lower) {
        return lower.startsWith("add task") || lower.startsWith("todo add") || lower.startsWith("list tasks")
                || lower.startsWith("show tasks") || lower.startsWith("done task") || lower.startsWith("remove task")
                || lower.startsWith("delete task");
    }

    private boolean isFocusRequest(String lower) {
        return lower.startsWith("start focus") || lower.startsWith("focus status") || lower.startsWith("end focus")
                || lower.startsWith("block site") || lower.startsWith("unblock site") || lower.startsWith("blocked sites");
    }

    private String extractSearchQuery(String input) {
        return input
                .replaceFirst("(?i)^search\\s+web\\s+", "")
                .replaceFirst("(?i)^google\\s+", "")
                .replaceFirst("(?i)^browse\\s+", "")
                .replaceFirst("(?i)^open\\s+website\\s+", "")
                .replaceFirst("(?i)^open\\s+site\\s+", "")
                .trim();
    }

    private String capabilitySummary() {
        return """
            I can help with day-to-day desktop tasks:
            - Notes: note buy milk, list notes, search notes work
            - Reminders: remind me to call mom at 7 PM, list reminders
            - App launch: open calculator, open https://github.com
            - Weather: forecast in Pollachi today
            - Utilities: what time is it, what is today's date, calculate 245/7
            - Desktop tools: system info, search web latest Java news
            - Daily briefing: good morning
            - Clipboard: show clipboard, save clipboard as note
            - Routines: start work mode
            - File helper: find file resume, recent files
            - Tasks: add task submit report, list tasks, done task 1
            - Focus: start focus 25, focus status, block site youtube.com
            - Safety: confirm open, action log
            """;
    }

    private String dailyBriefing() {
        List<Reminder> reminders = reminderService.listReminders();
        List<Note> notes = noteService.listNotes();

        String nextReminder = reminders.isEmpty()
                ? "No pending reminders"
                : reminders.get(0).summary() + " at " + formatInstant(reminders.get(0).getDueAt());
        String topNote = notes.isEmpty() ? "No recent notes" : notes.get(0).summary();

        logAction("Generated daily briefing");
        return "Good morning. " + LocalDate.now().format(DATE_FORMAT) + ", " + LocalTime.now().format(TIME_FORMAT) + ".\n"
                + "Next reminder: " + nextReminder + ".\n"
                + "Top note: " + topNote + ".";
    }

    private String handleClipboard(String input, String lower) {
        if (lower.contains("show clipboard") || lower.contains("read clipboard")) {
            String text = readClipboard();
            return text.isBlank() ? "Clipboard is empty or unavailable." : "Clipboard: " + truncate(text, 220);
        }

        if (lower.contains("save clipboard as note")) {
            String text = readClipboard();
            if (text.isBlank()) {
                return "Clipboard is empty or unavailable.";
            }
            Note note = noteService.createNote(text);
            logAction("Saved clipboard to note " + note.getId());
            return "Saved clipboard as note " + note.getId() + ".";
        }

        if (lower.startsWith("copy ")) {
            String text = input.substring(5).trim();
            if (text.isBlank()) {
                return "Nothing to copy.";
            }
            writeClipboard(text);
            logAction("Copied text to clipboard");
            return "Copied to clipboard.";
        }

        return "Clipboard commands: show clipboard, save clipboard as note, copy <text>.";
    }

    private String handleRoutine(String lower) {
        if (lower.startsWith("start work mode") || lower.equals("work mode")) {
            List<String> targets = List.of("https://mail.google.com", "https://calendar.google.com", "https://github.com", "notepad");
            int count = 0;
            for (String target : targets) {
                String result = launcherService.launchApp(target).toLowerCase(Locale.ROOT);
                if (result.contains("launched") || result.contains("opening")) {
                    count++;
                }
            }
            logAction("Started work mode");
            return "Work mode started. Opened " + count + " tools.";
        }

        if (lower.startsWith("shutdown work mode")) {
            logAction("Requested shutdown work mode");
            return "Automated app-closing is not enabled yet for safety. Say: open task manager if needed.";
        }

        return "Routine commands: start work mode, shutdown work mode.";
    }

    private String draftEmail(String input) {
        String topic = input.replaceFirst("(?i)^draft\\s+email\\s+", "")
                .replaceFirst("(?i)^write\\s+email\\s+", "")
                .replaceFirst("(?i)^compose\\s+email\\s+", "")
                .trim();
        if (topic.isBlank()) {
            topic = "project update";
        }

        String body = "Subject: " + toTitleCase(topic) + "\n\n"
                + "Hi,\n\n"
                + "I wanted to share an update regarding " + topic + ".\n"
                + "Please let me know if you need any additional details.\n\n"
                + "Thanks,\n"
                + "[Your Name]";

        writeClipboard(body);
        logAction("Drafted email for " + topic);
        return "Email draft copied to clipboard:\n\n" + body;
    }

    private String handleFiles(String input, String lower) {
        if (lower.startsWith("open downloads")) {
            Path downloads = Path.of(System.getProperty("user.home"), "Downloads");
            logAction("Opened Downloads");
            return launcherService.launchApp(downloads.toString());
        }

        if (lower.startsWith("recent files")) {
            Path downloads = Path.of(System.getProperty("user.home"), "Downloads");
            if (!Files.isDirectory(downloads)) {
                return "Downloads folder not found.";
            }
            try (Stream<Path> stream = Files.list(downloads)) {
                List<Path> files = stream.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(path -> path.toFile().lastModified(), Comparator.reverseOrder()))
                        .limit(5)
                        .toList();
                if (files.isEmpty()) {
                    return "No recent files found in Downloads.";
                }
                StringBuilder sb = new StringBuilder("Recent files:\n");
                for (Path path : files) {
                    sb.append("- ").append(path.getFileName()).append("\n");
                }
                return sb.toString().trim();
            } catch (IOException exception) {
                return "Could not read recent files right now.";
            }
        }

        String query = input.replaceFirst("(?i)^find\\s+file\\s+", "")
                .replaceFirst("(?i)^open\\s+file\\s+", "")
                .trim();
        if (query.isBlank()) {
            return "Use: find file <name> or open file <name>.";
        }

        List<Path> matches = findFiles(query, 5);
        if (matches.isEmpty()) {
            return "No files found for: " + query;
        }

        if (lower.startsWith("open file")) {
            Path top = matches.get(0);
            logAction("Opened file " + top.getFileName());
            return launcherService.launchApp(top.toString());
        }

        StringBuilder sb = new StringBuilder("Matching files:\n");
        for (Path match : matches) {
            sb.append("- ").append(match).append("\n");
        }
        return sb.toString().trim();
    }

    private List<Path> findFiles(String query, int limit) {
        String q = query.toLowerCase(Locale.ROOT);
        List<Path> roots = List.of(
                Path.of(System.getProperty("user.home"), "Downloads"),
                Path.of(System.getProperty("user.home"), "Documents"),
                Path.of(System.getProperty("user.home"), "Desktop"));

        List<Path> results = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root, 4)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).contains(q))
                        .limit(limit - results.size())
                        .forEach(results::add);
            } catch (IOException ignored) {
            }
            if (results.size() >= limit) {
                break;
            }
        }
        return results;
    }

    private String handleTasks(String input, String lower) {
        synchronized (TODO_ITEMS) {
            if (lower.startsWith("add task") || lower.startsWith("todo add")) {
                String text = input.replaceFirst("(?i)^add\\s+task\\s+", "")
                        .replaceFirst("(?i)^todo\\s+add\\s+", "")
                        .trim();
                if (text.isBlank()) {
                    return "Use: add task <text>.";
                }
                int id = TODO_ITEMS.stream().mapToInt(TodoItem::id).max().orElse(0) + 1;
                TODO_ITEMS.add(new TodoItem(id, text, false));
                logAction("Added task " + id);
                return "Task added: " + id + ". " + text;
            }

            if (lower.startsWith("done task")) {
                int id = extractNumber(lower);
                for (int i = 0; i < TODO_ITEMS.size(); i++) {
                    TodoItem item = TODO_ITEMS.get(i);
                    if (item.id() == id) {
                        TODO_ITEMS.set(i, new TodoItem(item.id(), item.text(), true));
                        logAction("Completed task " + id);
                        return "Task " + id + " marked done.";
                    }
                }
                return "Task not found.";
            }

            if (lower.startsWith("remove task") || lower.startsWith("delete task")) {
                int id = extractNumber(lower);
                boolean removed = TODO_ITEMS.removeIf(item -> item.id() == id);
                return removed ? "Removed task " + id + "." : "Task not found.";
            }

            if (TODO_ITEMS.isEmpty()) {
                return "No tasks yet. Use: add task <text>.";
            }

            StringBuilder sb = new StringBuilder("Tasks:\n");
            for (TodoItem item : TODO_ITEMS) {
                sb.append(item.done() ? "[DONE] " : "[TODO] ")
                        .append(item.id())
                        .append(". ")
                        .append(item.text())
                        .append("\n");
            }
            return sb.toString().trim();
        }
    }

    private String handleFocus(String input, String lower) {
        if (lower.startsWith("start focus")) {
            int mins = extractNumber(lower);
            if (mins <= 0) {
                mins = 25;
            }
            focusEndsAt = Instant.now().plus(Duration.ofMinutes(mins));
            logAction("Started focus " + mins + " min");
            return "Focus started for " + mins + " minutes.";
        }

        if (lower.startsWith("focus status")) {
            if (focusEndsAt == null || focusEndsAt.isBefore(Instant.now())) {
                return "Focus mode is not active.";
            }
            Duration left = Duration.between(Instant.now(), focusEndsAt);
            return "Focus active. " + left.toMinutes() + " minutes left.";
        }

        if (lower.startsWith("end focus")) {
            focusEndsAt = null;
            logAction("Ended focus");
            return "Focus mode ended.";
        }

        if (lower.startsWith("block site")) {
            String domain = input.replaceFirst("(?i)^block\\s+site\\s+", "").trim().toLowerCase(Locale.ROOT);
            if (domain.isBlank()) {
                return "Use: block site youtube.com";
            }
            BLOCKED_DOMAINS.add(domain);
            return "Blocked site: " + domain;
        }

        if (lower.startsWith("unblock site")) {
            String domain = input.replaceFirst("(?i)^unblock\\s+site\\s+", "").trim().toLowerCase(Locale.ROOT);
            if (domain.isBlank()) {
                return "Use: unblock site youtube.com";
            }
            BLOCKED_DOMAINS.remove(domain);
            return "Unblocked site: " + domain;
        }

        if (lower.startsWith("blocked sites")) {
            return BLOCKED_DOMAINS.isEmpty() ? "No blocked sites." : "Blocked sites: " + String.join(", ", BLOCKED_DOMAINS);
        }

        return "Focus commands: start focus 25, focus status, end focus, block site <domain>.";
    }

    private String confirmOpen() {
        if (pendingUrlForConfirmation == null || pendingUrlForConfirmation.isBlank()) {
            return "No pending URL to confirm.";
        }
        String url = pendingUrlForConfirmation;
        pendingUrlForConfirmation = null;
        logAction("Confirmed open for " + url);
        return launcherService.launchApp(url);
    }

    private boolean isTrusted(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return false;
            }
            String lower = host.toLowerCase(Locale.ROOT);
            for (String trusted : TRUSTED_DOMAINS) {
                if (lower.equals(trusted) || lower.endsWith("." + trusted)) {
                    return true;
                }
            }
        } catch (RuntimeException exception) {
            return false;
        }
        return false;
    }

    private boolean isBlocked(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        for (String blocked : BLOCKED_DOMAINS) {
            if (lower.contains(blocked)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeUrl(String value) {
        String url = value.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        return url;
    }

    private String actionLogSummary() {
        synchronized (ACTION_LOG) {
            if (ACTION_LOG.isEmpty()) {
                return "No actions recorded yet.";
            }
            StringBuilder sb = new StringBuilder("Recent actions:\n");
            ACTION_LOG.forEach(item -> sb.append("- ").append(item).append("\n"));
            return sb.toString().trim();
        }
    }

    private void logAction(String action) {
        synchronized (ACTION_LOG) {
            ACTION_LOG.addFirst(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + action);
            while (ACTION_LOG.size() > MAX_ACTION_LOG) {
                ACTION_LOG.removeLast();
            }
        }
    }

    private String readClipboard() {
        try {
            Object value = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            return value == null ? "" : value.toString();
        } catch (UnsupportedFlavorException | IOException | IllegalStateException exception) {
            return "";
        }
    }

    private void writeClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (IllegalStateException exception) {
            // Clipboard temporarily unavailable.
        }
    }

    private int extractNumber(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "unknown";
        }
        return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, max) + "...";
    }

    private String toTitleCase(String value) {
        String[] parts = value.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private String systemStatus() {
        Runtime runtime = Runtime.getRuntime();
        long maxMb = runtime.maxMemory() / (1024 * 1024);
        long totalMb = runtime.totalMemory() / (1024 * 1024);
        long freeMb = runtime.freeMemory() / (1024 * 1024);
        long usedMb = totalMb - freeMb;

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMs);

        String osName = System.getProperty("os.name", "unknown");
        String osVersion = System.getProperty("os.version", "unknown");
        String javaVersion = System.getProperty("java.version", "unknown");

        return String.format(
                Locale.ROOT,
                "System status:%n"
                + "- OS: %s %s%n"
                + "- Java: %s%n"
                + "- App uptime: %dh %dm%n"
                + "- JVM memory: used %d MB / total %d MB (max %d MB)",
                osName,
                osVersion,
                javaVersion,
                uptime.toHours(),
                uptime.toMinutesPart(),
                usedMb,
                totalMb,
                maxMb);
    }
}
