package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.edithj.launcher.AppLauncherService;

class EmailCommandHandlerTest {

    @Test
    void parseRequest_extractsBodyAndContactNameWithoutEmail() {
        EmailCommandHandler handler = new EmailCommandHandler(new RecordingLauncherService());

        EmailCommandHandler.ParsedEmailRequest parsed = handler.parseRequest("email hello to Krithick");

        assertEquals("", parsed.recipientEmail());
        assertEquals("Krithick", parsed.contactName());
        assertEquals("hello", parsed.body());
    }

    @Test
    void handle_buildsMailtoUrlForExplicitEmailAndBody() {
        RecordingLauncherService launcherService = new RecordingLauncherService();
        EmailCommandHandler handler = new EmailCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext(
                "send an email to test@example.com saying I'll be late",
                "send an email to test@example.com saying I'll be late",
                "typed"));

        assertEquals("mailto:test@example.com?subject=Message%20from%20EDITH-J&body=I%27ll%20be%20late", launcherService.launchedTarget());
        assertTrue(response.contains("Opening your email client with a draft message to test@example.com."));
    }

    @Test
    void handle_buildsMailtoUrlWithSubjectAndBody() {
        RecordingLauncherService launcherService = new RecordingLauncherService();
        EmailCommandHandler handler = new EmailCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext(
                "draft an email to hr@example.com with subject 'Leave request' and say I need a day off",
                "draft an email to hr@example.com with subject 'Leave request' and say I need a day off",
                "typed"));

        assertEquals("mailto:hr@example.com?subject=Leave%20request&body=I%20need%20a%20day%20off", launcherService.launchedTarget());
        assertTrue(response.contains("Opening your email client with a draft message to hr@example.com."));
    }

    @Test
    void handle_returnsClarificationWhenBodyMissingAndDoesNotLaunch() {
        RecordingLauncherService launcherService = new RecordingLauncherService();
        EmailCommandHandler handler = new EmailCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext("draft an email to hr@example.com", "draft an email to hr@example.com", "typed"));

        assertTrue(response.contains("didn’t catch the message text") || response.contains("didn't catch the message text"));
        assertTrue(launcherService.launchedTarget().isBlank());
    }

    @Test
    void buildMailtoUrl_usesDefaultSubjectAndEncodesBody() {
        EmailCommandHandler handler = new EmailCommandHandler(new RecordingLauncherService());
        EmailCommandHandler.ParsedEmailRequest request = new EmailCommandHandler.ParsedEmailRequest("", "Krithick", "Message from EDITH-J", "hello world");

        assertEquals("mailto:?subject=Message%20from%20EDITH-J&body=hello%20world", handler.buildMailtoUrl(request));
    }

    private static final class RecordingLauncherService extends AppLauncherService {

        private String launchedTarget = "";

        @Override
        public String launchApp(String target) {
            launchedTarget = target == null ? "" : target;
            return "Launched " + launchedTarget;
        }

        String launchedTarget() {
            return launchedTarget;
        }
    }
}
