package com.edithj.commands;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edithj.assistant.IntentType;

public class FileSearchCommandHandler implements CommandHandler {

    private static final Pattern FILE_TYPE_PATTERN = Pattern.compile("\\b(?:pdf|docx|doc|txt|xlsx|pptx|csv|md|rtf|jpg|jpeg|png|zip)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCOPE_PATTERN = Pattern.compile("\\b(workspace|documents|downloads|desktop|home|my documents|my downloads)\\b", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final FileSearchService fileSearchService;

    public FileSearchCommandHandler() {
        this(new FileSearchService());
    }

    FileSearchCommandHandler(FileSearchService fileSearchService) {
        this.fileSearchService = fileSearchService;
    }

    @Override
    public IntentType intentType() {
        return IntentType.FILE_SEARCH;
    }

    @Override
    public String handle(CommandContext context) {
        String payload = sanitizePayload(context);
        if (payload.isBlank()) {
            return "Tell me what files you'd like to find, for example: find resume or locate pdf files.";
        }

        String normalized = payload.toLowerCase(Locale.ROOT);
        String fileType = parseFileType(normalized);
        String scope = parseScope(normalized);
        String query = normalizeSearchQuery(normalized, fileType, scope);
        if (query.isBlank() && fileType.isBlank()) {
            return "Tell me what files you'd like to find, for example: find resume or locate pdf files.";
        }

        List<FileSearchService.FileSearchResult> results = fileSearchService.search(query, fileType, scope);
        if (results.isEmpty()) {
            return "I couldn't find any files matching '" + payload.trim() + "'.";
        }

        StringBuilder answer = new StringBuilder();
        answer.append("Found ").append(results.size()).append(" files matching '")
                .append(query.isBlank() ? payload.trim() : query).append("':\n");
        for (FileSearchService.FileSearchResult result : results) {
            String uri = Path.of(result.path()).toUri().toString();
            answer.append("- ")
                    .append(result.name())
                    .append(" (")
                    .append(result.extension().isBlank() ? "file" : result.extension().toUpperCase(Locale.ROOT))
                    .append(", ")
                    .append(formatSize(result.sizeBytes()))
                    .append(", modified ")
                    .append(DATE_FORMATTER.format(result.lastModified()))
                    .append(") ")
                    .append(uri)
                    .append("\n");
        }
        return answer.toString().trim();
    }

    private String normalizeSearchQuery(String normalizedInput, String fileType, String scope) {
        String query = normalizedInput;
        query = query.replaceAll("(?i)\\b(find|locate|search for|search|look for|look up)\\b", "");
        if (!fileType.isBlank()) {
            query = query.replaceAll("(?i)\\b" + Pattern.quote(fileType) + "\\b", "");
        }
        if (!scope.isBlank()) {
            query = query.replaceAll("(?i)\\b" + Pattern.quote(scope) + "\\b", "");
        }
        query = query.replaceAll("\\b(files|documents|document|pdfs|docs|reports|spreadsheets|presentations)\\b", "");
        return query.trim();
    }

    private String parseFileType(String normalizedInput) {
        Matcher matcher = FILE_TYPE_PATTERN.matcher(normalizedInput);
        if (matcher.find()) {
            return matcher.group(0).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private String parseScope(String normalizedInput) {
        Matcher matcher = SCOPE_PATTERN.matcher(normalizedInput);
        if (matcher.find()) {
            String found = matcher.group(1).toLowerCase(Locale.ROOT);
            if (found.equals("my documents")) {
                return "documents";
            }
            if (found.equals("my downloads")) {
                return "downloads";
            }
            if (found.equals("home")) {
                return "workspace";
            }
            return found;
        }
        return "";
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ENGLISH, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.ENGLISH, "%.1f MB", mb);
        }
        return String.format(Locale.ENGLISH, "%.1f GB", mb / 1024.0);
    }
}
