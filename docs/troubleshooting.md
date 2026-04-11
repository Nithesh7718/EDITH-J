# Troubleshooting

## Groq replies are missing

- Set `GROQ_API_KEY` in your environment.
- If the key is unset, chat falls back to a friendly configuration message.
- You can still use notes, reminders, launcher commands, utilities, and voice routing without the key.

## Notes or reminders do not persist

- Check that the process can write to `~/.edith-j/data/`.
- Delete the JSON file only if you want to reset local data; the app recreates it automatically.

## Speech input returns typed fallback

- This is expected when transcription is unavailable or blank.
- Type the fallback response when prompted and the assistant will route it normally.

## Launcher commands do nothing

- Launcher behavior is OS-specific.
- On Windows, common apps are mapped through `WindowsLauncher`; on other platforms, the generic launcher path is used.

## Maven or JavaFX issues

- Use Java 17.
- Run `mvn clean test` first to verify the project compiles before launching the UI.
- If JavaFX runtime errors appear, confirm the JavaFX dependencies are available in your Maven cache.
