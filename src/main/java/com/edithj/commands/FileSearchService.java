package com.edithj.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.edithj.ai.AiConfig;
import com.edithj.config.AppConfig;

public class FileSearchService {

    private static final String DEFAULT_ALLOWED_DIRS = "workspace,documents,downloads,desktop";
    private static final String DEFAULT_EXCLUDED_EXTENSIONS = ".tmp,.log";
    private static final int DEFAULT_MAX_RESULTS = 10;
    private static final int MAX_SEARCH_DEPTH = 6;

    private final AiConfig aiConfig;
    private final int maxResults;
    private final List<String> excludedExtensions;
    private final List<String> allowedDirs;

    public FileSearchService() {
        this(AiConfig.load(), AppConfig.load(),
                AppConfig.load().get("edith.search.maxResults", String.valueOf(DEFAULT_MAX_RESULTS)),
                AppConfig.load().get("edith.search.allowedDirs", DEFAULT_ALLOWED_DIRS),
                AppConfig.load().get("edith.search.excludeExtensions", DEFAULT_EXCLUDED_EXTENSIONS));
    }

    FileSearchService(AiConfig aiConfig, AppConfig appConfig, String maxResultsValue, String allowedDirsValue, String excludeExtensionsValue) {
        this.aiConfig = Objects.requireNonNull(aiConfig, "aiConfig");
        this.maxResults = parseMaxResults(maxResultsValue);
        this.allowedDirs = parseAllowedDirs(allowedDirsValue);
        this.excludedExtensions = parseExcludedExtensions(excludeExtensionsValue);
    }

    public List<FileSearchResult> search(String query, String fileType, String scope) {
        String normalizedQuery = normalize(query);
        String normalizedExtension = normalizeFileType(fileType);
        List<Path> roots = resolveSearchRoots(scope);
        if (roots.isEmpty()) {
            return List.of();
        }

        List<FileSearchResult> results = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root, MAX_SEARCH_DEPTH)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> isAllowedByRoot(path, roots))
                        .filter(path -> matchesExtension(path, normalizedExtension))
                        .filter(path -> matchesQuery(path, normalizedQuery))
                        .forEach(path -> addSearchResult(results, path));
            } catch (IOException ignored) {
                // Best-effort scan.
            }
        }

        return results.stream()
                .sorted((a, b) -> b.lastModified().compareTo(a.lastModified()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    private int parseMaxResults(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_MAX_RESULTS;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(1, parsed);
        } catch (NumberFormatException exception) {
            return DEFAULT_MAX_RESULTS;
        }
    }

    private List<String> parseAllowedDirs(String configuration) {
        if (configuration == null || configuration.isBlank()) {
            return List.of("workspace", "documents", "downloads", "desktop");
        }
        return Arrays.stream(configuration.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private List<String> parseExcludedExtensions(String configuration) {
        if (configuration == null || configuration.isBlank()) {
            return List.of();
        }
        return Arrays.stream(configuration.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::normalizeFileType)
                .distinct()
                .toList();
    }

    private List<Path> resolveSearchRoots(String scope) {
        List<Path> roots = new ArrayList<>();
        if (scope != null && !scope.isBlank()) {
            Path root = resolveScopeRoot(scope.trim().toLowerCase(Locale.ROOT));
            if (root != null && Files.isDirectory(root)) {
                roots.add(root);
                return roots;
            }
        }

        for (String allowedDir : allowedDirs) {
            Path root = resolveScopeRoot(allowedDir);
            if (root != null && Files.isDirectory(root)) {
                roots.add(root);
            }
        }

        return roots;
    }

    private Path resolveScopeRoot(String token) {
        String home = System.getProperty("user.home", "");
        if (token == null) {
            return null;
        }

        return switch (token) {
            case "workspace" ->
                aiConfig.workspaceDir();
            case "documents" ->
                Path.of(home, "Documents");
            case "downloads" ->
                Path.of(home, "Downloads");
            case "desktop" ->
                Path.of(home, "Desktop");
            default -> {
                if (token.isBlank()) {
                    yield null;
                }
                Path candidate = Path.of(token);
                yield candidate.isAbsolute() ? candidate.toAbsolutePath().normalize() : null;
            }
        };
    }

    private boolean isAllowedByRoot(Path path, List<Path> roots) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        for (Path root : roots) {
            if (normalizedPath.startsWith(root.toAbsolutePath().normalize())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesExtension(Path path, String extension) {
        if (extension.isBlank()) {
            return !isExcluded(path);
        }
        return extension.equals(normalizeFileType(getFileExtension(path))) && !isExcluded(path);
    }

    private boolean isExcluded(Path path) {
        String extension = normalizeFileType(getFileExtension(path));
        return excludedExtensions.contains(extension);
    }

    private boolean matchesQuery(Path path, String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return true;
        }
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.contains(normalizedQuery);
    }

    private void addSearchResult(List<FileSearchResult> results, Path path) {
        try {
            long size = Files.size(path);
            Instant modified = Files.getLastModifiedTime(path).toInstant();
            String name = path.getFileName().toString();
            String extension = normalizeFileType(getFileExtension(path));
            FileSearchResult result = new FileSearchResult(path.toAbsolutePath().toString(), name, extension, size, modified);
            if (!results.contains(result)) {
                results.add(result);
            }
        } catch (IOException ignored) {
            // best effort
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFileType(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith(".")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    private String getFileExtension(Path path) {
        if (path == null) {
            return "";
        }
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        if (idx <= 0 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx + 1);
    }

    public record FileSearchResult(String path, String name, String extension, long sizeBytes, Instant lastModified) {

        public FileSearchResult {
            path = path == null ? "" : path;
            name = name == null ? "" : name;
            extension = extension == null ? "" : extension;
            lastModified = lastModified == null ? Instant.EPOCH : lastModified;
        }
    }
}
