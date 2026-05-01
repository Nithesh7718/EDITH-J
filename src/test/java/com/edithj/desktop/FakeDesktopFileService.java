package com.edithj.desktop;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FakeDesktopFileService implements DesktopFileService {

    private Path downloadsPath = Path.of("C:/fake/Downloads");
    private final List<Path> recentFiles = new ArrayList<>();
    private final List<Path> searchableFiles = new ArrayList<>();

    public void setDownloadsPath(Path downloadsPath) {
        this.downloadsPath = downloadsPath;
    }

    public void setRecentFiles(List<Path> files) {
        recentFiles.clear();
        recentFiles.addAll(files);
    }

    public void setSearchableFiles(List<Path> files) {
        searchableFiles.clear();
        searchableFiles.addAll(files);
    }

    @Override
    public Path downloadsPath() {
        return downloadsPath;
    }

    @Override
    public List<Path> listRecentFiles(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return new ArrayList<>(recentFiles.stream().limit(limit).toList());
    }

    @Override
    public List<Path> findFiles(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        return new ArrayList<>(searchableFiles.stream()
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .limit(limit)
                .toList());
    }
}
