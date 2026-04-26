package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.edithj.launcher.FakeLauncher;

class EmailCommandHandlerTest {

    @Test
    void parseRequest_extractsBodyAndContactNameWithoutEmail() {
        EmailCommandHandler handler = new EmailCommandHandler(new FakeLauncher());

        EmailCommandHandler.ParsedEmailRequest parsed = handler.parseRequest("email hello to Krithick");

        assertEquals("", parsed.recipientEmail());
        assertEquals("Krithick", parsed.contactName());
        assertEquals("hello", parsed.body());
    }

    @Test
    void handle_buildsMailtoUrlForExplicitEmailAndBody() {
        FakeLauncher launcherService = new FakeLauncher();
        EmailCommandHandler handler = new EmailCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext(
                "send an email to test@example.com saying I'll be late",
                "send an email to test@example.com saying I'll be late",
                "typed"));

        assertEquals("mailto:test@example.com?subject=Message%20from%20EDITH-J&body=I%27ll%20be%20late", launcherService.lastOpenedUrl());
        assertTrue(response.contains("Opening your email client with a draft message to test@example.com."));
    }

    @Test
    void handle_buildsMailtoUrlWithSubjectAndBody() {
        FakeLauncher launcherService = new FakeLauncher();
        EmailCommandHandler handler = new EmailCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext(
                "draft an email to hr@example.com with subject 'Leave request' and say I need a day off",
                "draft an email to hr@example.com with subject 'Leave request' and say I need a day off",
                "typed"));

        assertEquals("mailto:hr@example.com?subject=Leave%20request&body=I%20need%20a%20day%20off", launcherService.lastOpenedUrl());
        assertTrue(response.contains("Opening your email client with a draft message to hr@example.com."));
    }

    @Test
    void handle_returnsClarificationWhenBodyMissingAndDoesNotLaunch() {
        FakeLauncher launcherService = new FakeLauncher();
        EmailCommandHandler handler = new EmailCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext("draft an email to hr@example.com", "draft an email to hr@example.com", "typed"));

        assertTrue(response.contains("didn’t catch the message text") || response.contains("didn't catch the message text"));
        assertTrue(launcherService.lastOpenedTarget().isBlank());
    }

    @Test
    void buildMailtoUrl_usesDefaultSubjectAndEncodesBody() {
        EmailCommandHandler handler = new EmailCommandHandler(new FakeLauncher());
        EmailCommandHandler.ParsedEmailRequest request = new EmailCommandHandler.ParsedEmailRequest(
                "",
                "Krithick",
                "Message from EDITH-J",
                "hello world",
                false,
                false);

        assertEquals("mailto:?subject=Message%20from%20EDITH-J&body=hello%20world", handler.buildMailtoUrl(request));
    }

    @Test
    void parseRequest_extractsRegardingQuotedSubjectAndContact() {
        EmailCommandHandler handler = new EmailCommandHandler(new FakeLauncher());

        EmailCommandHandler.ParsedEmailRequest parsed = handler.parseRequest("send a email reg \"project update\" to krithick");

        assertEquals("krithick", parsed.contactName());
        assertEquals("project update", parsed.subject());
        assertTrue(parsed.subjectExplicit());
        assertTrue(parsed.body().isBlank());
    }

    @Test
    void handle_returnsFollowUpWhenRegardingSubjectIsEmpty() {
        FakeLauncher launcherService = new FakeLauncher();
        EmailCommandHandler handler = new EmailCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext(
                "send a email reg \"  \" to krithick",
                "send a email reg \"  \" to krithick",
                "typed"));

        assertEquals("What should the email be about?", response);
        assertTrue(launcherService.lastOpenedTarget().isBlank());
    }

    @Test
    void handle_returnsBodyPromptWhenSubjectProvidedViaRegarding() {
        FakeLauncher launcherService = new FakeLauncher();
        EmailCommandHandler handler = new EmailCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext(
                "send an email regarding \"project update\" to krithick",
                "send an email regarding \"project update\" to krithick",
                "typed"));

        assertTrue(response.contains("I’ll draft an email to krithick") || response.contains("I'll draft an email to krithick"));
        assertTrue(response.contains("subject \"project update\""));
        assertTrue(response.contains("What would you like the body to say?"));
        assertTrue(launcherService.lastOpenedTarget().isBlank());
    }

}
