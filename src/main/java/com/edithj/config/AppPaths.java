package com.edithj.config;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Centralized path resolution for development and packaged Windows installs.
 */
public final class AppPaths {

    private static final String APP_DIR_NAME = "EDITH-J";
    private static final String LEGACY_DIR_NAME = ".edith-j";
    private static final String CONFIG_FILE_NAME = "edith.properties";
    private static final String CONFIG_TEMPLATE_NAME = "edith.properties.example";
    private static final String DEFAULT_VOSK_MODEL_DIR = "vosk-model-small-en-us-0.15";

    private AppPaths() {
    }

    public static void bootstrapSystemProperties() {
        System.setProperty("edithj.install.dir", installDirectory().toString());
        System.setProperty("edithj.config.dir", configDirectory().toString());
        System.setProperty("edithj.data.dir", dataDirectory().toString());
        System.setProperty("edithj.logs.dir", logsDirectory().toString());
        System.setProperty("edithj.temp.dir", tempDirectory().toString());
        System.setProperty("edithj.workspace.dir", defaultWorkspaceDirectory().toString());
        createDirectoriesQuietly(configDirectory());
        createDirectoriesQuietly(dataDirectory());
        createDirectoriesQuietly(logsDirectory());
        createDirectoriesQuietly(tempDirectory());
    }

    public static void ensureRuntimeDirectories() {
        createDirectories(configDirectory());
        createDirectories(dataDirectory());
        createDirectories(logsDirectory());
        createDirectories(tempDirectory());
        createDirectories(defaultWorkspaceDirectory());
    }

    public static Path installDirectory() {
        String packagedLauncher = System.getProperty("jpackage.app-path");
        if (packagedLauncher != null && !packagedLauncher.isBlank()) {
            Path launcher = Path.of(packagedLauncher).toAbsolutePath().normalize();
            Path parent = launcher.getParent();
            if (parent != null) {
                return parent;
            }
        }

        try {
            Path codeSource = Path.of(AppPaths.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .toAbsolutePath()
                    .normalize();
            if (Files.isRegularFile(codeSource)) {
                return codeSource.getParent();
            }
            return codeSource;
        } catch (URISyntaxException | RuntimeException exception) {
            return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        }
    }

    public static Path configDirectory() {
        String appData = firstNonBlank(System.getenv("APPDATA"));
        if (appData != null) {
            return Path.of(appData, APP_DIR_NAME).toAbsolutePath().normalize();
        }
        return legacyHomeDirectory().resolve("config");
    }

    public static Path dataDirectory() {
        String localAppData = firstNonBlank(System.getenv("LOCALAPPDATA"));
        if (localAppData != null) {
            return Path.of(localAppData, APP_DIR_NAME, "data").toAbsolutePath().normalize();
        }
        return legacyHomeDirectory().resolve("data");
    }

    public static Path logsDirectory() {
        String localAppData = firstNonBlank(System.getenv("LOCALAPPDATA"));
        if (localAppData != null) {
            return Path.of(localAppData, APP_DIR_NAME, "logs").toAbsolutePath().normalize();
        }
        return legacyHomeDirectory().resolve("logs");
    }

    public static Path tempDirectory() {
        String localAppData = firstNonBlank(System.getenv("LOCALAPPDATA"));
        if (localAppData != null) {
            return Path.of(localAppData, APP_DIR_NAME, "temp").toAbsolutePath().normalize();
        }
        return legacyHomeDirectory().resolve("temp");
    }

    public static Path defaultWorkspaceDirectory() {
        String userHome = System.getProperty("user.home", ".");
        Path documents = Path.of(userHome, "Documents");
        if (Files.isDirectory(documents)) {
            return documents.resolve("EDITH-workspace").toAbsolutePath().normalize();
        }
        return Path.of(userHome, "EDITH-workspace").toAbsolutePath().normalize();
    }

    public static Path userConfigPath() {
        return configDirectory().resolve(CONFIG_FILE_NAME);
    }

    public static Path userConfigTemplatePath() {
        return configDirectory().resolve(CONFIG_TEMPLATE_NAME);
    }

    public static Path bundledConfigDirectory() {
        return applicationContentDirectory().resolve("conf").toAbsolutePath().normalize();
    }

    public static Path bundledConfigTemplatePath() {
        return bundledConfigDirectory().resolve(CONFIG_TEMPLATE_NAME);
    }

    public static Path bundledModelDirectory() {
        return applicationContentDirectory().resolve("models").resolve(DEFAULT_VOSK_MODEL_DIR).toAbsolutePath().normalize();
    }

    public static Path workingDirectoryConfigPath() {
        return Path.of(CONFIG_FILE_NAME).toAbsolutePath().normalize();
    }

    public static Path resolveAgainstInstallDirectory(String value) {
        return resolvePath(value, applicationContentDirectory());
    }

    public static Path resolveAgainstConfigDirectory(String value) {
        return resolvePath(value, configDirectory());
    }

    public static Path resolveAgainstDataDirectory(String value) {
        return resolvePath(value, dataDirectory());
    }

    public static Path resolveAgainstWorkspaceDefault(String value) {
        return resolvePath(value, defaultWorkspaceDirectory().getParent());
    }

    private static Path legacyHomeDirectory() {
        return Path.of(System.getProperty("user.home", "."), LEGACY_DIR_NAME).toAbsolutePath().normalize();
    }

    private static Path applicationContentDirectory() {
        Path installDir = installDirectory();
        Path appDir = installDir.resolve("app");
        if (Files.isDirectory(appDir)) {
            return appDir.toAbsolutePath().normalize();
        }
        return installDir;
    }

    private static Path resolvePath(String value, Path baseDirectory) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            return baseDirectory;
        }

        Path candidate = Path.of(trimmed);
        if (candidate.isAbsolute()) {
            return candidate.toAbsolutePath().normalize();
        }

        Path base = baseDirectory == null
                ? Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()
                : baseDirectory.toAbsolutePath().normalize();
        return base.resolve(candidate).toAbsolutePath().normalize();
    }

    private static void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create directory: " + path, exception);
        }
    }

    private static void createDirectoriesQuietly(Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception ignored) {
            // Logging should remain best-effort during early bootstrap.
        }
    }

    private static String firstNonBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
