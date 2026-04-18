package com.edithj.desktop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class SystemDesktopFileService implements DesktopFileService {

    @Override
    public Path downloadsPath() {
        return Path.of(System.getProperty("user.home"), "Downloads");
    }

    @Override
    public List<Path> listRecentFiles(int limit) {
        Path downloads = downloadsPath();
        if (!Files.isDirectory(downloads)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(downloads)) {
            return stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.toFile().lastModified(), Comparator.reverseOrder()))
                    .limit(limit)
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    @Override
    public List<Path> findFiles(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        List<Path> roots = List.of(
                downloadsPath(),
                Path.of(System.getProperty("user.home"), "Documents"),
                Path.of(System.getProperty("user.home"), "Desktop"));

        List<Path> results = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }

            try (Stream<Path> stream = Files.walk(root, 4)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                        .limit(limit - results.size())
                        .forEach(results::add);
            } catch (IOException ignored) {
                // Best-effort scan.
            }

            if (results.size() >= limit) {
                break;
            }
        }

        return results;
    }
}
