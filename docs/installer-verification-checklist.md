# Installer Verification Checklist

## Build verification

1. Run `mvn clean package -P windows-installer`.
2. Confirm the installer exists under `target/installer/`.
3. Confirm the shaded JAR exists under `target/`.
4. Confirm `target/jpackage-input/models/vosk-model-small-en-us-0.15/` exists during packaging.
5. Confirm `target/jpackage-input/conf/edith.properties.example` exists during packaging.

## Install verification

1. Install the generated `.exe` on a Windows machine that does not have the repo checked out.
2. Launch EDITH-J from the Start menu shortcut.
3. Confirm no console window flashes during normal launch.
4. Confirm the default browser opens only when `app.auto-open-browser=true`.

## First-run verification

1. Confirm `%APPDATA%\EDITH-J\edith.properties.example` is created.
2. Confirm `%LOCALAPPDATA%\EDITH-J\data` is created.
3. Confirm `%LOCALAPPDATA%\EDITH-J\logs` is created.
4. Confirm the SQLite database file is created automatically.
5. Confirm the app still starts when `%APPDATA%\EDITH-J\edith.properties` does not exist.

## Functional verification

1. Open `http://localhost:8080` and confirm the React UI loads.
2. Confirm `/api/health` returns `{"status":"ok","service":"EDITH-J"}`.
3. Create a note and restart the app; confirm it persists.
4. Create a reminder and restart the app; confirm it persists.
5. Confirm missing AI API keys do not crash the app and instead produce a helpful response.
6. Confirm voice status is available even when the Vosk model is missing, and that the app reports speech as unavailable instead of crashing.

## Failure-path verification

1. Occupy port `8080` and confirm EDITH-J shows a useful startup error.
2. Temporarily remove the packaged Vosk model and confirm startup still works without speech.
3. Temporarily break the workspace path in config and confirm EDITH-J falls back to the default workspace.
4. Temporarily remove the frontend assets from a test build and confirm startup fails with a clear packaging error.
