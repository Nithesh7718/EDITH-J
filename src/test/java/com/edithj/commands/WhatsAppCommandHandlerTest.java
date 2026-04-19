package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.edithj.launcher.FakeLauncher;

class WhatsAppCommandHandlerTest {

    @Test
    void parseRequest_extractsMessageWithoutContact() {
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(new FakeLauncher());

        WhatsAppCommandHandler.ParsedWhatsAppRequest parsed = handler.parseRequest("whatsapp hello");

        assertEquals("hello", parsed.message());
        assertTrue(parsed.contactName().isBlank());
    }

    @Test
    void parseRequest_extractsQuotedMessageAndContact() {
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(new FakeLauncher());

        WhatsAppCommandHandler.ParsedWhatsAppRequest parsed
                = handler.parseRequest("send a \"hello\" message to Krithick via whatsapp");

        assertEquals("hello", parsed.message());
        assertEquals("Krithick", parsed.contactName());
        assertTrue(!parsed.callIntent());
    }

    @Test
    void parseRequest_detectsWhatsappCallIntentAndContact() {
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(new FakeLauncher());

        WhatsAppCommandHandler.ParsedWhatsAppRequest parsed
                = handler.parseRequest("make a call to krithick via whatsapp");

        assertTrue(parsed.callIntent());
        assertEquals("krithick", parsed.contactName());
        assertEquals("", parsed.message());
    }

    @Test
    void handle_returnsClarificationWhenMessageMissingAndDoesNotLaunch() {
        FakeLauncher launcherService = new FakeLauncher();
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext("whatsapp", "whatsapp", "typed"));

        assertTrue(response.contains("didn’t catch the text") || response.contains("didn't catch the text"));
        assertTrue(launcherService.lastOpenedTarget().isBlank());
    }

    @Test
    void handle_buildsWhatsAppWebUrlAndLaunchesIt() {
        FakeLauncher launcherService = new FakeLauncher();
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext("whatsapp hello world", "whatsapp hello world", "typed"));

        assertEquals("https://wa.me/?text=hello%20world", launcherService.lastOpenedUrl());
        assertTrue(response.contains("Opening WhatsApp Web with your message: \"hello world\"."));
    }

    @Test
    void handle_callIntent_returnsLimitationAndDoesNotLaunchWhatsApp() {
        FakeLauncher launcherService = new FakeLauncher();
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext(
                "make a call to Krithick via whatsapp",
                "make a call to Krithick via whatsapp",
                "typed"));

        assertTrue(response.contains("can’t start WhatsApp calls") || response.contains("can't start WhatsApp calls"));
        assertTrue(response.contains("Krithick"));
        assertTrue(launcherService.lastOpenedTarget().isBlank());
    }

    @Test
    void buildWhatsAppWebUrl_encodesSpacesAsPercentTwenty() {
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(new FakeLauncher());

        assertEquals("https://wa.me/?text=hello%20world", handler.buildWhatsAppWebUrl("hello world"));
    }

}
