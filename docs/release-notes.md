# Release Notes

## 0.1.0-SNAPSHOT

- Initial JavaFX desktop assistant scaffold.
- Added assistant intent routing for notes, reminders, launch, weather, utilities, desktop tools, and fallback chat.
- Added file-backed note and reminder persistence.
- Added speech capture with typed fallback support.
- Added lightweight JavaFX navigation and view controllers.
- Added JUnit 5 + Mockito test coverage for the core service and routing paths.

### Known limitations

- Weather and Groq-backed chat depend on network access.
- Some UI views remain thin wrappers over the service layer and are intentionally minimal.
