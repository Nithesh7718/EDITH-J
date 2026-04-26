package com.edithj.commands;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.edithj.assistant.AssistantTelemetry;
import com.edithj.desktop.FakeClipboardService;
import com.edithj.desktop.FakeDesktopFileService;
import com.edithj.desktop.session.FocusSessionState;
import com.edithj.desktop.session.InMemoryFocusSessionState;
import com.edithj.desktop.session.InMemoryTaskSessionState;
import com.edithj.desktop.session.TaskSessionState;
import com.edithj.launcher.FakeLauncher;
import com.edithj.notes.InMemoryNoteRepository;
import com.edithj.notes.NoteService;
import com.edithj.reminders.InMemoryReminderRepository;
import com.edithj.reminders.ReminderService;
import com.edithj.config.PreferencesService;

// IDE sync trigger
class DesktopToolsCommandHandlerTest {

    private CommandHandler.CommandContext context(String input) {
        return new CommandHandler.CommandContext(input, input, "typed");
    }

    @Test
    void handle_withScreenshotReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                new FakeDesktopFileService());
        String response = handler.handle(context("screenshot"));

        assertTrue(response != null && !response.isBlank());
    }

    @Test
    void handle_withClipboardReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                new FakeDesktopFileService());
        String response = handler.handle(context("clipboard"));

        assertTrue(response != null && !response.isBlank());
    }

    @Test
    void handle_withoutPayloadReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                new FakeDesktopFileService());
        String response = handler.handle(new CommandHandler.CommandContext("", "", "typed"));

        assertTrue(!response.isBlank());
    }

    @Test
    void intentType_returnsDesktopTools() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                new FakeDesktopFileService());

        assertTrue(handler.intentType().toString().contains("DESKTOP"));
    }

    @Test
    void handle_withBlankPayloadReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                new FakeDesktopFileService());
        String response = handler.handle(new CommandHandler.CommandContext("   ", "   ", "typed"));

        assertTrue(!response.isBlank());
    }

    @Test
    void handle_workModeDisabled_doesNotLaunchAnyTarget() {
        FakeLauncher fakeLauncher = new FakeLauncher();
        DesktopToolsCommandHandler handler = newHandler(fakeLauncher, false, new FakeClipboardService(),
                new FakeDesktopFileService());

        String response = handler.handle(context("start work mode"));

        assertEquals("Launcher demo commands are disabled in this configuration.", response);
        assertEquals(0, fakeLauncher.launchCount());
    }

    @Test
    void handle_workModeEnabled_launchesDemoTargetsWithFakeLauncher() {
        PreferencesService.instance().setDevSmokeLaunchersEnabled(true);
        FakeLauncher fakeLauncher = new FakeLauncher();
        DesktopToolsCommandHandler handler = newHandler(fakeLauncher, true, new FakeClipboardService(),
                new FakeDesktopFileService());

        String response = handler.handle(context("start work mode"));

        assertTrue(response.contains("Work mode started"));
        assertEquals(4, fakeLauncher.launchCount());
        assertEquals("notepad", fakeLauncher.lastOpenedApp());
    }

    @Test
    void handle_showClipboard_readsFromFakeClipboardOnly() {
        FakeClipboardService clipboard = new FakeClipboardService();
        clipboard.setText("sample clipboard text");
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, clipboard, new FakeDesktopFileService());

        String response = handler.handle(context("show clipboard"));

        assertTrue(response.contains("sample clipboard text"));
    }

    @Test
    void handle_copyWhenClipboardUnavailable_returnsHelpfulMessage() {
        FakeClipboardService clipboard = new FakeClipboardService();
        clipboard.setWritable(false);
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, clipboard, new FakeDesktopFileService());

        String response = handler.handle(context("copy hello"));

        assertEquals("Clipboard is unavailable right now.", response);
    }

    @Test
    void handle_saveClipboardAsNote_withEmptyClipboardReturnsMessage() {
        FakeClipboardService clipboard = new FakeClipboardService();
        clipboard.setText("");
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, clipboard, new FakeDesktopFileService());

        String response = handler.handle(context("save clipboard as note"));

        assertEquals("Clipboard is empty or unavailable.", response);
    }

    @Test
    void handle_recentFiles_usesFakeFileService() {
        FakeDesktopFileService fileService = new FakeDesktopFileService();
        fileService.setRecentFiles(List.of(Path.of("C:/fake/Downloads/report.pdf"), Path.of("C:/fake/Downloads/notes.txt")));
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(), fileService);

        String response = handler.handle(context("recent files"));

        assertTrue(response.contains("report.pdf"));
        assertTrue(response.contains("notes.txt"));
    }

    @Test
    void handle_openFile_withNoMatchesReturnsHelpfulMessage() {
        FakeDesktopFileService fileService = new FakeDesktopFileService();
        fileService.setSearchableFiles(List.of(Path.of("C:/fake/Documents/design.docx")));
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(), fileService);

        String response = handler.handle(context("open file budget"));

        assertEquals("No files found for: budget", response);
    }

    @Test
    void handle_addTaskWithoutTextReturnsGuidance() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                new FakeDesktopFileService());

        String response = handler.handle(context("add task"));

        assertEquals("Use: add task <text>.", response);
    }

    @Test
    void handle_tasksFlowPersistsWithinSingleHandlerInstance() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                new FakeDesktopFileService());

        String added = handler.handle(context("add task submit report"));
        String firstList = handler.handle(context("list tasks"));
        String completed = handler.handle(context("done task 1"));
        String secondList = handler.handle(context("list tasks"));

        assertEquals("Task added: 1. submit report", added);
        assertTrue(firstList.contains("[TODO] 1. submit report"));
        assertEquals("Task 1 marked done.", completed);
        assertTrue(secondList.contains("[DONE] 1. submit report"));
    }

    @Test
    void handle_focusFlowTransitionsCorrectly() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                new FakeDesktopFileService());

        String started = handler.handle(context("start focus 25"));
        String status = handler.handle(context("focus status"));
        String ended = handler.handle(context("end focus"));
        String statusAfterEnd = handler.handle(context("focus status"));

        assertEquals("Focus started for 25 minutes.", started);
        assertTrue(status.contains("Focus active."));
        assertEquals("Focus mode ended.", ended);
        assertEquals("Focus mode is not active.", statusAfterEnd);
    }

    @Test
    void handle_openFileWhenDisabled_returnsFlagMessage() {
        FakeDesktopFileService fileService = new FakeDesktopFileService();
        fileService.setSearchableFiles(List.of(Path.of("C:/fake/Documents/budget.xlsx")));

        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                fileService, false, true);

        String response = handler.handle(context("open file budget"));

        assertEquals("File open commands are disabled in this configuration.", response);
    }

    @Test
    void handle_copyWhenDisabled_returnsFlagMessage() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                new FakeDesktopFileService(), true, false);

        String response = handler.handle(context("copy hello"));

        assertEquals("Clipboard write commands are disabled in this configuration.", response);
    }

    @Test
    void handle_telemetryStatus_reportsCurrentCounters() {
        AssistantTelemetry telemetry = AssistantTelemetry.instance();
        telemetry.reset();
        telemetry.recordClarificationPrompt();
        telemetry.recordClarificationPrompt();
        telemetry.recordWorldCircuitOpenHit();
        telemetry.recordLocalKbEmptyHit();

        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                new FakeDesktopFileService());

        String response = handler.handle(context("telemetry status"));

        assertTrue(response.contains("Assistant telemetry"));
        assertTrue(response.contains("Clarification prompts: 2"));
        assertTrue(response.contains("World circuit-open hits: 1"));
        assertTrue(response.contains("Local KB empty hits: 1"));
    }

    @Test
    void handle_telemetryReset_clearsCounters() {
        AssistantTelemetry telemetry = AssistantTelemetry.instance();
        telemetry.reset();
        telemetry.recordClarificationPrompt();
        telemetry.recordWorldCircuitOpenHit();
        telemetry.recordLocalKbEmptyHit();

        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false, new FakeClipboardService(),
                new FakeDesktopFileService());

        String resetResponse = handler.handle(context("telemetry reset"));
        String statusResponse = handler.handle(context("telemetry status"));

        assertEquals("Assistant telemetry reset.", resetResponse);
        assertTrue(statusResponse.contains("Clarification prompts: 0"));
        assertTrue(statusResponse.contains("World circuit-open hits: 0"));
        assertTrue(statusResponse.contains("Local KB empty hits: 0"));
    }

    private DesktopToolsCommandHandler newHandler(
            FakeLauncher fakeLauncher,
            boolean smokeLaunchersEnabled,
            FakeClipboardService clipboard,
            FakeDesktopFileService fileService) {
        return newHandler(fakeLauncher, smokeLaunchersEnabled, clipboard, fileService, true, true);
    }

    private DesktopToolsCommandHandler newHandler(
            FakeLauncher fakeLauncher,
            boolean smokeLaunchersEnabled,
            FakeClipboardService clipboard,
            FakeDesktopFileService fileService,
            boolean fileOpenEnabled,
            boolean clipboardWriteEnabled) {
        NoteService noteService = new NoteService(new InMemoryNoteRepository());
        ReminderService reminderService = new ReminderService(new InMemoryReminderRepository());
        TaskSessionState taskSessionState = new InMemoryTaskSessionState();
        FocusSessionState focusSessionState = new InMemoryFocusSessionState();
        return new DesktopToolsCommandHandler(fakeLauncher, clipboard, fileService, noteService, reminderService,
                taskSessionState, focusSessionState, smokeLaunchersEnabled, fileOpenEnabled, clipboardWriteEnabled);
    }
}
