package com.edithj.desktop;

import java.nio.file.Path;
import java.util.List;

public interface DesktopFileService {

    Path downloadsPath();

    List<Path> listRecentFiles(int limit);

    List<Path> findFiles(String query, int limit);
}
