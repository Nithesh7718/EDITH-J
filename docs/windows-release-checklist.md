# Windows Release Checklist

## Pre-release

1. Confirm `mvn clean package` succeeds on Windows with Java 25.
2. Confirm `mvn clean package -P windows-installer` succeeds on Windows with WiX Toolset v3.14 installed.
3. Confirm the frontend was rebuilt and the shaded JAR contains `public/index.html`.
4. Confirm `target_old_*` folders and other stale build artifacts are not present in the release branch.
5. Confirm `edith.properties.example` matches the current runtime keys.
6. Confirm `src/main/packaging/edith-j.ico` is the intended release icon.

## Artifact naming

Upload Windows artifacts with stable names:

1. `EDITH-J-<version>-windows-x64.exe`
2. `EDITH-J-<version>-windows-x64.msi`
3. `EDITH-J-<version>-windows-x64-app-image.zip`

## GitHub Release

1. Create a Git tag for the release version.
2. Build the installer artifacts from that tagged commit.
3. Upload the `.exe` installer first.
4. Optionally upload `.msi` and `app-image.zip`.
5. Include release notes that state:
   - Java runtime is bundled
   - config lives under `%APPDATA%\EDITH-J`
   - data/logs live under `%LOCALAPPDATA%\EDITH-J`
   - AI providers still require user API keys

## Post-upload

1. Download the uploaded installer from GitHub Releases onto a clean Windows machine.
2. Run the installer verification checklist before marking the release live.
