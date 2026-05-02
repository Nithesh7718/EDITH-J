package com.edithj.commands;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edithj.ai.AiConfig;
import com.edithj.ai.ChatProvider;
import com.edithj.ai.ProviderFactory;
import com.edithj.assistant.IntentType;
import com.edithj.launcher.AppLauncherService;
import com.edithj.resilience.FailureType;
import com.edithj.resilience.HealthMonitorRegistry;
import com.edithj.resilience.HealthSignal;
import com.edithj.resilience.IncidentSeverity;

public class DesktopAutomationCommandHandler implements CommandHandler {

    private static final Pattern PROVIDER_HINT_PATTERN = Pattern.compile("(?i)\\buse\\s+(groq|gemini|openai|sarvam)\\b");

    private final AppLauncherService launcherService;
    private final AiConfig aiConfig;
    private final ProviderFactory providerFactory;

    public DesktopAutomationCommandHandler() {
        this(new AppLauncherService(), AiConfig.load());
    }

    public DesktopAutomationCommandHandler(AppLauncherService launcherService, AiConfig aiConfig) {
        this(launcherService, aiConfig, new ProviderFactory(aiConfig));
    }

    DesktopAutomationCommandHandler(AppLauncherService launcherService, AiConfig aiConfig, ProviderFactory providerFactory) {
        this.launcherService = launcherService;
        this.aiConfig = aiConfig;
        this.providerFactory = providerFactory;
    }

    @Override
    public IntentType intentType() {
        return IntentType.DESKTOP_AUTOMATION;
    }

    @Override
    public String handle(CommandContext context) {
        String input = context == null || context.normalizedInput() == null
                ? ""
                : context.normalizedInput().trim();
        String lower = input.toLowerCase(Locale.ROOT);

        if (input.isBlank()) {
            return usage();
        }

        if (lower.startsWith("open app ") || lower.startsWith("launch app ")) {
            return handleOpenApp(input);
        }

        if (lower.startsWith("play music") || lower.startsWith("play my playlist")) {
            return handlePlayMusic();
        }

        if (lower.startsWith("search the web") || lower.startsWith("web search")) {
            return handleWebSearch(input);
        }

        if (lower.startsWith("file open ")) {
            return handleFileOpen(input);
        }

        if (lower.startsWith("file create text ")) {
            return handleFileCreateText(input);
        }

        if (lower.startsWith("file rename ")) {
            return handleFileRename(input);
        }

        if (lower.startsWith("file move ")) {
            return handleFileMove(input);
        }

        if (lower.startsWith("write code ")) {
            return handleWriteGeneratedFile(input, true);
        }

        if (lower.startsWith("write document ")) {
            return handleWriteGeneratedFile(input, false);
        }

        return usage();
    }

    private String handleOpenApp(String input) {
        String app = input.replaceFirst("(?i)^open\\s+app\\s+", "")
                .replaceFirst("(?i)^launch\\s+app\\s+", "")
                .trim();
        if (app.isBlank()) {
            return "Use: open app <application name>.";
        }

        String override = aiConfig.launchOverride(app.toLowerCase(Locale.ROOT));
        String target = override.isBlank() ? app : override;
        return launcherService.launchApp(target);
    }

    private String handlePlayMusic() {
        String musicUrl = aiConfig.property("edith.automation.musicUrl", "https://music.youtube.com");
        return launcherService.launchApp(musicUrl);
    }

    private String handleWebSearch(String input) {
        String query = input.replaceFirst("(?i)^search\\s+the\\s+web\\s+for\\s+", "")
                .replaceFirst("(?i)^search\\s+the\\s+web\\s+", "")
                .replaceFirst("(?i)^web\\s+search\\s+", "")
                .trim();

        if (query.isBlank()) {
            return "Use: search the web for <query>.";
        }

        String searchUrl = "https://www.google.com/search?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8);
        return launcherService.launchApp(searchUrl);
    }

    private String handleFileOpen(String input) {
        String relative = input.replaceFirst("(?i)^file\\s+open\\s+", "").trim();
        if (relative.isBlank()) {
            return "Use: file open <relative path>.";
        }

        Path target = safeWorkspacePath(relative);
        if (target == null) {
            return "For safety, file operations are restricted to edith.ai.workspaceDir.";
        }
        if (!Files.exists(target) || Files.isDirectory(target)) {
            return "File not found: " + relative;
        }

        return launcherService.launchApp(target.toString());
    }

    private String handleFileCreateText(String input) {
        Matcher matcher = Pattern.compile("(?i)^file\\s+create\\s+text\\s+(.+?)(?:\\s+with\\s+(.+))?$")
                .matcher(input.trim());
        if (!matcher.matches()) {
            return "Use: file create text <relative path> with <content>.";
        }

        String relative = matcher.group(1).trim();
        String content = matcher.group(2) == null ? "" : matcher.group(2).trim();

        Path target = safeWorkspacePath(relative);
        if (target == null) {
            return "For safety, file operations are restricted to edith.ai.workspaceDir.";
        }

        try {
            Files.createDirectories(target.getParent());
            if (Files.exists(target)) {
                return "File already exists. Rename or move it before creating: " + relative;
            }
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return "Created file: " + workspaceRelativeDisplay(target);
        } catch (IOException exception) {
            return "I could not create that file right now.";
        }
    }

    private String handleFileRename(String input) {
        Matcher matcher = Pattern.compile("(?i)^file\\s+rename\\s+(.+?)\\s+to\\s+(.+)$").matcher(input.trim());
        if (!matcher.matches()) {
            return "Use: file rename <source> to <target>.";
        }

        String from = matcher.group(1).trim();
        String to = matcher.group(2).trim();

        Path source = safeWorkspacePath(from);
        Path target = safeWorkspacePath(to);
        if (source == null || target == null) {
            return "For safety, file operations are restricted to edith.ai.workspaceDir.";
        }
        if (!Files.exists(source) || Files.isDirectory(source)) {
            return "Source file does not exist: " + from;
        }
        if (Files.exists(target)) {
            return "Target already exists. Rename aborted to avoid destructive overwrite.";
        }

        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            return "Renamed file to: " + workspaceRelativeDisplay(target);
        } catch (IOException exception) {
            return "I could not rename that file right now.";
        }
    }

    private String handleFileMove(String input) {
        Matcher matcher = Pattern.compile("(?i)^file\\s+move\\s+(.+?)\\s+to\\s+(.+)$").matcher(input.trim());
        if (!matcher.matches()) {
            return "Use: file move <source> to <target path>.";
        }

        String from = matcher.group(1).trim();
        String to = matcher.group(2).trim();

        Path source = safeWorkspacePath(from);
        Path target = safeWorkspacePath(to);
        if (source == null || target == null) {
            return "For safety, file operations are restricted to edith.ai.workspaceDir.";
        }
        if (!Files.exists(source) || Files.isDirectory(source)) {
            return "Source file does not exist: " + from;
        }
        if (Files.exists(target)) {
            return "Target already exists. Move aborted to avoid destructive overwrite.";
        }

        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target);
            return "Moved file to: " + workspaceRelativeDisplay(target);
        } catch (IOException exception) {
            return "I could not move that file right now.";
        }
    }

    private String handleWriteGeneratedFile(String input, boolean codeMode) {
        String commandPrefix = codeMode ? "write code" : "write document";
        String remainder = codeMode
                ? input.replaceFirst("(?i)^write\\s+code\\s+", "").trim()
                : input.replaceFirst("(?i)^write\\s+document\\s+", "").trim();
        int colonIndex = remainder.indexOf(':');
        if (colonIndex <= 0) {
            return "Use: " + commandPrefix + " <relative file path>: <instructions>.";
        }

        String relativePath = remainder.substring(0, colonIndex).trim();
        String request = remainder.substring(colonIndex + 1).trim();
        if (relativePath.isBlank() || request.isBlank()) {
            return "Use: " + commandPrefix + " <relative file path>: <instructions>.";
        }

        Path target = safeWorkspacePath(withDefaultExtension(relativePath, codeMode));
        if (target == null) {
            return "For safety, file operations are restricted to edith.ai.workspaceDir.";
        }

        String providerOverride = extractProviderHint(request);
        String cleanRequest = removeProviderHint(request);

        ChatProvider provider = providerOverride == null
                ? providerFactory.createProvider()
                : providerFactory.createProvider(providerOverride);

        String prompt = (codeMode
                ? "Generate production-ready code. Return only file content."
                : "Generate a polished document. Return only file content.")
                + "\nTask: " + cleanRequest;

        String generated = provider.generateReply(prompt);
        if (generated == null || generated.isBlank()) {
            HealthMonitorRegistry.instance().registerHealthSignal(new HealthSignal(
                    "automation.ai",
                    FailureType.PROVIDER,
                    IncidentSeverity.HIGH,
                    "Generated content was empty from the provider.",
                    Instant.now(),
                    Map.of("provider", provider.providerName(), "path", target.toString())));
            return "The model returned no content for this request.";
        }
        if (looksLikeProviderError(generated)) {
            HealthMonitorRegistry.instance().registerHealthSignal(new HealthSignal(
                    "automation.ai",
                    FailureType.PROVIDER,
                    IncidentSeverity.MEDIUM,
                    "Provider returned an error message during code generation.",
                    Instant.now(),
                    Map.of("provider", provider.providerName(), "response", generated)));
            return generated;
        }

        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, generated, StandardCharsets.UTF_8);
            return "Saved generated "
                    + (codeMode ? "code" : "document")
                    + " using "
                    + provider.providerName()
                    + " to "
                    + workspaceRelativeDisplay(target)
                    + ".";
        } catch (IOException exception) {
            HealthMonitorRegistry.instance().registerHealthSignal(new HealthSignal(
                    "automation.storage",
                    FailureType.STORAGE,
                    IncidentSeverity.MEDIUM,
                    "Generated file was created but could not be written to disk.",
                    Instant.now(),
                    Map.of("path", target.toString(), "error", exception.getMessage())));
            return "I generated content, but I could not save the file.";
        }
    }

    private String extractProviderHint(String text) {
        Matcher matcher = PROVIDER_HINT_PATTERN.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1).toLowerCase(Locale.ROOT) : null;
    }

    private String removeProviderHint(String text) {
        if (text == null) {
            return "";
        }
        return PROVIDER_HINT_PATTERN.matcher(text).replaceAll("").replaceAll("\\s+", " ").trim();
    }

    private String withDefaultExtension(String path, boolean codeMode) {
        String trimmed = path == null ? "" : path.trim();
        if (trimmed.isBlank()) {
            return codeMode ? "generated-code.txt" : "generated-document.md";
        }

        Path parsed = Path.of(trimmed);
        String fileName = parsed.getFileName() == null ? "" : parsed.getFileName().toString();
        if (fileName.contains(".")) {
            return trimmed;
        }

        return trimmed + (codeMode ? ".txt" : ".md");
    }

    private boolean looksLikeProviderError(String response) {
        String lower = response.toLowerCase(Locale.ROOT);
        return lower.startsWith("missing api key")
                || lower.startsWith("invalid provider endpoint")
                || lower.startsWith("unable to reach")
                || lower.contains("request failed with http")
                || lower.startsWith("i could not generate a response");
    }

    private Path safeWorkspacePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        Path workspaceRoot = aiConfig.workspaceDir().toAbsolutePath().normalize();
        Path candidate = Path.of(rawPath.trim());
        if (candidate.isAbsolute()) {
            return null;
        }

        Path normalized = workspaceRoot.resolve(candidate).normalize();
        return normalized.startsWith(workspaceRoot) ? normalized : null;
    }

    private String workspaceRelativeDisplay(Path absolutePath) {
        Path workspaceRoot = aiConfig.workspaceDir().toAbsolutePath().normalize();
        try {
            return workspaceRoot.relativize(absolutePath.toAbsolutePath().normalize()).toString();
        } catch (RuntimeException exception) {
            return absolutePath.toString();
        }
    }

    private String usage() {
        return "Desktop automation commands: "
                + "open app <name>, play music, search the web for <query>, "
                + "file open <path>, file create text <path> with <content>, "
                + "file rename <from> to <to>, file move <from> to <to>, "
                + "write code <path>: <instructions>, write document <path>: <instructions>.";
    }
}
